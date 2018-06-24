(ns midje-nrepl.formatter
  (:require [rewrite-clj.custom-zipper.utils
             :refer
             [remove-left-while remove-right-while]]
            [rewrite-clj.zip :as zip]
            [rewrite-clj.zip.whitespace :as whitespace]))

(defn- throw-exception [type message & others]
  (throw (ex-info (name type) (merge {:type    type
                                      :message message} (apply hash-map others)))))

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

(defn- table-must-be-well-formed [number-of-columns cells]
  (if (zero? (mod (count cells) number-of-columns))
    cells
    (throw-exception ::malformed-table "Table isn't well formed: not all rows have the same number of cells")))

(defn- table-header? [value]
  (boolean (re-find #"^\?" value)))

(defn- number-of-columns [cells]
  (let [number-of-headers (count (take-while table-header? cells))]
    (if-not (zero? number-of-headers)
      number-of-headers
      (throw-exception ::no-table-headers "Table has no headers"))))

(defn paddings-for-cells [cells options]
  (let [number-of-columns (number-of-columns cells)]
    (->> cells
         (table-must-be-well-formed number-of-columns)
         (partition number-of-columns)
         (apply map (partial paddings-for-column options))
         identify-leftmost-and-rightmost-cells
         (apply interleave))))

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

(defn- sexpr-must-be-tabular [zloc]
  (let [sexpr (zip/sexpr zloc)]
    (if (and (symbol? sexpr) (= (symbol (name sexpr)) 'tabular))
      zloc
      (throw-exception ::no-tabular "Sexpr must be a tabular fact" :sexpr (zip/root-string zloc)))))

(defn- move-to-first-header [sexpr]
  (-> (zip/of-string sexpr)
      zip/down
      sexpr-must-be-tabular
      (zip/find (comp table-header? zip/string))))

(defn format-tabular
  ([sexpr]
   (format-tabular sexpr {}))
  ([sexpr options]
   {:pre [sexpr]}
   (let [options (merge {:alignment      :right
                         :border-spacing 1
                         :indent-size    2} options)
         zloc    (move-to-first-header sexpr)]
     (loop [zloc     zloc
            paddings (paddings-for-tabular-sexpr zloc options)]
       (if-not (zip/right zloc)
         (zip/root-string (align zloc (first paddings) options))
         (recur (zip/right (align zloc (first paddings) options)) (rest paddings)))))))
