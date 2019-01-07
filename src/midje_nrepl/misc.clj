(ns midje-nrepl.misc
  (:require             [orchard.classpath :as classpath]))

(defn dependency-in-classpath?
  "Return true if a given dependency is in the project's classpath, or false otherwise."
  [^String dep-name]
  (let [pattern (re-pattern (str "/" dep-name ".*\\.jar$"))]
    (->> (classpath/classpath)
         (map #(.getPath %))
         (some (partial re-find pattern))
         boolean)))

(defn parse-options
  "Takes a map of options (key -> value) and a map of parsers (key ->
  parser function). Applies each parser function to the corresponding
  value in the options map, returning a new map of key -> parsed
  values."
  [options parsers-map]
  (reduce (fn [result [key parse-fn]]
            (if-let [value (get options key)]
              (assoc result key (parse-fn value))
              result)
            ) {} parsers-map))
