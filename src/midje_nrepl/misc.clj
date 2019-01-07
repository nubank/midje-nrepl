(ns midje-nrepl.misc
  (:require             [orchard.classpath :as classpath])
  (:import (java.time Duration LocalDateTime)))

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

(defn now
  "Return a java.time.LocalDateTime object representing the current date and time."
  []
  (LocalDateTime/now))

(defn duration-between
  "Return a java.time.Duration object representing the duration between two temporal objects."
  [start end]
  (Duration/between start end))
