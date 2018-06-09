(ns midje-nrepl.formatter
  (:require [rewrite-clj.custom-zipper.utils :refer [remove-right-while]]
            [rewrite-clj.zip :as zip]
            [rewrite-clj.zip.whitespace :as whitespace]))

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
       (apply interleave)))

(def ^:private whitespace-but-not-linebreak? #(and (not (zip/linebreak? %))
                                                   (zip/whitespace? %)))

(defn- align [zloc {:keys [padding-left padding-right]} {:keys [border-spacing]}]
  (-> zloc
      (remove-right-while whitespace-but-not-linebreak?)
      (whitespace/insert-space-left (+ 2 padding-left))
      (whitespace/insert-space-right (+ padding-right 1))
      zip/right))

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
        (recur (align zloc (first paddings) options) (rest paddings))))))
