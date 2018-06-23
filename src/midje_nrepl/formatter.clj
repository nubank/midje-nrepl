(ns midje-nrepl.formatter
  (:require [rewrite-clj.custom-zipper.utils
             :refer
             [remove-left-while remove-right-while]]
            [rewrite-clj.zip :as zip]
            [rewrite-clj.zip.whitespace :as whitespace]))

(defn- identify-leftmost-and-rightmost-cells [table]
  (let [leftmost-column  (first table)
        rightmost-column (last table)
        other-columns    (butlast (rest table))]
    (-> (map #(assoc % :leftmost-cell? true) leftmost-column)
        (cons other-columns)
        (concat (list (map #(assoc % :rightmost-cell? true) rightmost-column))))))

(defn- centered [text-length padding-size width]
  (let [padding-left  (int (/ padding-size 2))
        padding-right (- width (+ text-length padding-left))]
    {:padding-left  padding-left
     :padding-right padding-right}))

(defn paddings [text alignment width]
  (let [length       (count text)
        padding-size (- width length)]
    (case alignment
      :center (centered length padding-size width)
      :left   {:padding-left 0 :padding-right padding-size}
      :right  {:padding-left padding-size :padding-right 0})))

(defn- column-width [cells]
  (->> cells (apply max-key count) count))

(defn- paddings-for-column [{:keys [alignment]} & cells]
  (let [width (column-width cells)]
    (map #(paddings % alignment width) cells)))

(defn- column-header? [value]
  (boolean (re-find #"^\?" value)))

(defn- number-of-columns [values]
  (count (take-while column-header? values)))

(defn paddings-for-cells [cells options]
  (->> cells
       (partition (number-of-columns cells))
       (apply map (partial paddings-for-column options))
       identify-leftmost-and-rightmost-cells
       (apply interleave)))

(def ^:private whitespace-but-not-linebreak? #(and (not (zip/linebreak? %))
                                                   (zip/whitespace? %)))

(defn- align [zloc {:keys [leftmost-cell? rightmost-cell? padding-left padding-right]} {:keys [border-spacing indent-size]}]
  (let [offset-left (if leftmost-cell? (+ indent-size padding-left) padding-left)]
    (-> zloc
        (cond-> leftmost-cell? (remove-left-while whitespace-but-not-linebreak?))
        (remove-right-while whitespace-but-not-linebreak?)
        (whitespace/insert-space-left offset-left)
        (cond-> (not rightmost-cell?) (whitespace/insert-space-right (+ padding-right border-spacing))))))

(defn- paddings-for-tabular-sexpr [zloc options]
  (loop [zloc  zloc
         cells []]
    (if (zip/end? zloc)
      (paddings-for-cells cells options)
      (recur (zip/right zloc) (conj cells (zip/string zloc))))))

(defn- move-to-first-header [sexpr]
  (-> (zip/of-string sexpr)
      zip/down
      (zip/find (comp column-header? zip/string))))

(defn format-tabular [sexpr options]
  (let [zloc (move-to-first-header sexpr)]
    (loop [zloc     zloc
           paddings (paddings-for-tabular-sexpr zloc options)]
      (if-not (zip/right zloc)
        (zip/root-string (align zloc (first paddings) options))
        (recur (zip/right (align zloc (first paddings) options)) (rest paddings))))))
