(ns midje-nrepl.formatter
  (:require [rewrite-clj.zip :as zip]
            [rewrite-clj.node :as node]
            [clojure.string :as string]))

(defn center [text width]
  (let [length       (count text)
        padding-size (- width length)
        padding-left (int (+ length (/ padding-size 2)))]
    (->> text
         (format (str "%" padding-left "s"))
         (format (str "%-" width "s")))))

(defn align [text alignment width]
  (case alignment
    :center (center text width)
    :left   (format (str "%-" width "s") text)
    :right  (format (str "%" width "s") text)))

(defn- column-width [cells]
  (->> cells (apply max-key count) count))

(defn- align-column [{:keys [alignment center-headers?]} & [header & others :as cells]]
  (let [width       (column-width cells)
        align-cells (partial map #(align % alignment width))]
    (if-not center-headers?
      (align-cells cells)
      (cons (center header width) (align-cells others)))))

(defn- column-header? [value]
  nu/tap value
  (boolean (re-find #"^\?" value)))

(defn- number-of-columns [values]
  (count (take-while column-header? values)))

(defn align-table [cells options]
  (let [number-of-columns (number-of-columns cells)]
    (->> cells
         (partition number-of-columns)
         (apply map (partial align-column options))
         (apply interleave))))

(defn- get-aligned-cells [zloc options]
  (loop [zloc zloc
         cells[]]
    (if (zip/end? zloc)
      (align-table cells options)
      (recur (zip/right zloc) (conj cells (zip/string zloc))))))

(defn format-tabular [sexp {:keys [alignment center-headers?] :or {alignment :right, center-headers? true} :as options}]
  (let [zipper (zip/of-string sexp)
        zloc (-> zipper
                 zip/down
                 (zip/find zip/right (comp column-header? zip/string)))
        aligned-cells         (get-aligned-cells zloc options)]
    (loop [zloc zloc
           aligned-cells aligned-cells]
      (if (zip/end? zloc)
        (zip/print-root zloc)
        (recur (zip/right zloc) (rest aligned-cells))))))

(def t "(tabular (fact \"about basic arithmetic operations\"
  (?operation ?a ?b) => ?result)
  ?operation ?a ?b ?result
  + 2 5 10
  + 10 4 14
  - 100 25 75
    * 123 69 8487
  / 15 8 15/8
  / 4284 126 34)")

(-> (zip/of-string t)
    zip/down
    zip/right
    zip/right)
