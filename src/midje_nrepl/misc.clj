(ns midje-nrepl.misc
  (:require [orchard.classpath :as classpath])
  (:import java.text.DecimalFormat
           [java.time Duration Instant]
           java.util.Locale))

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
              result)) {} parsers-map))

(defn now
  "Return a java.time.Instant object representing the current instant."
  []
  (Instant/now))

(defn duration-between
  "Return a java.time.Duration object representing the duration between two temporal objects."
  [start end]
  (Duration/between start end))

(def ^:private formatter
  "Instance of java.text.Decimalformat used internally to format decimal
  values."
  (let [decimal-format (DecimalFormat/getInstance (Locale/ENGLISH))]
    (.applyPattern decimal-format "#.##")
    decimal-format))

(defn format-decimal
  "Formats the decimal number as a string."
  [value]
  {:pre [value]}
  (.format formatter value))

(defn percent
  "Returns the decimal number as a string representing a percent value."
  [value]
  (str (format-decimal value) "%"))
