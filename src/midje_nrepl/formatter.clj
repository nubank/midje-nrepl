(ns midje-nrepl.formatter
  (:require [clojure.string :as string]
            [rewrite-clj.custom-zipper.utils
             :refer
             [remove-left-while remove-right-while]]
            [rewrite-clj.zip :as zip]
            [rewrite-clj.zip.whitespace :as whitespace]
            [clojure.string :as string]
            [orchard.misc :as misc]))

(defn- throw-exception [type message & others]
  (throw (ex-info (name type) (merge {:type    type
                                      :error-message message} (apply hash-map others)))))

(defn- identify-leftmost-and-rightmost-cells [{:keys [deliniated-header?]} table]
  (let [leftmost-column  (first table)
        rightmost-column (last table)
        other-columns    (butlast (rest table))]
    (-> (map (fn [leftmost-cell]
               (-> leftmost-cell
                   (assoc  :leftmost-cell? true)
                   (update :padding-left (if deliniated-header? inc identity)))) leftmost-column)
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

(defn- paddings-for-column [{:keys [alignment deliniated-header?]} & cells]
  (let [width (column-width cells)]
    (map #(paddings % alignment width) cells)))

(defn- table-must-be-well-formed [number-of-columns cells]
  (if (zero? (mod (count cells) number-of-columns))
    cells
    (throw-exception ::malformed-table "Table isn't well formed: not all rows have the same number of cells")))

(defn- table-header? [value]
  (boolean (re-find #"^\?|^\[\?" value)))

(defn- number-of-columns-fn [cells]
  (let [number-of-headers (count (take-while table-header? cells))]
    (if-not (zero? number-of-headers)
      number-of-headers
      (throw-exception ::no-table-headers "Table has no headers. Check if headers start with `?`"))))

(defn paddings-for-cells [cells options]
  (let [number-of-columns (number-of-columns-fn cells)]
    (->> cells
         (table-must-be-well-formed number-of-columns)
         (partition number-of-columns)
         (apply map (partial paddings-for-column options))
         (identify-leftmost-and-rightmost-cells options)
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

(defn- format-tabular-loop
  [zloc* paddings* options]
  (loop [zloc     zloc*
         paddings paddings*]
    (if-not (zip/right zloc)
      (zip/root-string (align zloc (first paddings) options))
      (recur (zip/right (align zloc (first paddings) options)) (rest paddings)))))

(defn- expand-header
  [zloc]
  (-> zloc zip/up zip/string (string/replace (zip/string zloc) (-> zloc zip/string (string/replace #"\[|\]" ""))) move-to-first-header))

(defn- deliniate-header
  [formatted-tabular]
  (-> formatted-tabular
      (string/replace #"(\n\s*)(\?[a-zA-Z]+)" "$1[$2")
      (string/replace #"\s\[\?" "[?")
      (string/replace #"(\?[a-z-_A-Z]+)\s*\n" "$1]\n")))

(defn format-tabular
  ([sexpr]
   (format-tabular sexpr {}))
  ([sexpr options]
   {:pre [sexpr]}
   (let [zloc* (move-to-first-header sexpr)
         deliniated-header? (boolean (some->> zloc* zip/string (re-find #"^\[\?")))
         zloc (if deliniated-header? (expand-header zloc*) zloc*)
         options (merge {:alignment      :right
                         :border-spacing 1
                         :indent-size    2
                         :deliniated-header? deliniated-header?} options)
         formatted-tabular (format-tabular-loop zloc (paddings-for-tabular-sexpr zloc options) options)]
     (if deliniated-header?
       (deliniate-header formatted-tabular)
       formatted-tabular))))
