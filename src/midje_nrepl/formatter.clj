(ns midje-nrepl.formatter
  (:require [rewrite-clj.zip :as zip]
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
  (boolean (re-find #"^\?" value)))

(defn- number-of-columns [values]
  (count (take-while column-header? values)))

(defn align-table [cells options]
  (let [number-of-columns (number-of-columns cells)]
    (->> cells
         (partition number-of-columns)
         (apply map (partial align-column options))
         (apply interleave))))
