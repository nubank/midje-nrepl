(ns midje-nrepl.formatter
  (:require [rewrite-clj.custom-zipper.utils :refer [remove-left-while remove-right-while]]
            [rewrite-clj.zip :as zip]
            [rewrite-clj.zip.whitespace :as whitespace]))

(defn- indicate-the-leftmost-cells [[leftmost-column :as table]]
  (-> (map #(assoc % :leftmost-cell? true) leftmost-column)
      (cons (rest table))))

(defn- centered [text-length padding-size width]
  (let [padding-left  (int (/ padding-size 2))
        padding-right (- width (+ text-length padding-left))]
    {:padding-left  padding-left
     :padding-right padding-right}))

(defn padding [text alignment width]
  (let [length       (count text)
        padding-size (- width length)]
    (case alignment
      :center (centered length padding-size width)
      :left   {:padding-left 0 :padding-right padding-size}
      :right  {:padding-left padding-size :padding-right 0})))

(defn- column-width [cells]
  (->> cells (apply max-key count) count))

(defn- paddings-for-column [{:keys [alignment center-headers?]} & [header & others :as cells]]
  (let [width             (column-width cells)
        padding-for-cells (partial map #(padding % alignment width))]
    (if-not center-headers?
      (padding-for-cells cells)
      (cons (padding header :center width) (padding-for-cells others)))))

(defn- column-header? [value]
  (boolean (re-find #"^\?" value)))

(defn- number-of-columns [values]
  (count (take-while column-header? values)))

(defn paddings-for-table [cells options]
  (->> cells
       (partition (number-of-columns cells))
       (apply map (partial paddings-for-column options))
       indicate-the-leftmost-cells
       (apply interleave)))

(def ^:private whitespace-but-not-linebreak? #(and (not (zip/linebreak? %))
                                                   (zip/whitespace? %)))

(defn- align [zloc {:keys [leftmost-cell? padding-left padding-right] :as p} {:keys [border-spacing indent-size]}]
  (let [left-space (if leftmost-cell? (+ indent-size padding-left) padding-left)]
    (-> zloc
        (cond-> leftmost-cell? (remove-left-while whitespace-but-not-linebreak?))
        (remove-right-while whitespace-but-not-linebreak?)
        (whitespace/insert-space-left left-space)
        (whitespace/insert-space-right (+ padding-right border-spacing)))))

(defn- get-aligned-cells [zloc options]
  (loop [zloc  zloc
         cells []]
    (if (zip/end? zloc)
      (paddings-for-table cells options)
      (recur (zip/right zloc) (conj cells (zip/string zloc))))))

(defn format-tabular [sexpr options]
  (let [zloc (-> (zip/of-string sexpr)
                 zip/down
                 (zip/find (comp column-header? zip/string)))]
    (loop [zloc     zloc
           paddings (get-aligned-cells zloc options)]
      (if-not (zip/right zloc)
        (zip/root-string (align zloc (first paddings) options))
        (recur (zip/right (align zloc (first paddings) options)) (rest paddings))))))

(def tabular1 "(tabular (fact \"about basic arithmetic operations\"
  (?operation ?a ?b) => ?result)
  ?operation ?a ?b ?result
  + 2 5 10
  + 10 4 14
  - 100 25 75
    * 123 69 8487
  / 15 8 15/8
  / 4284 126 34)")

(format-tabular tabular1 {:alignment       :right
                          :center-headers? true
                          :border-spacing  1
                          :indent-size     2})
