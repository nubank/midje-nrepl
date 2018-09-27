(ns midje-nrepl.helpers
  (:require [orchard.classpath :as classpath]))

(defn dep-in-classpath?
  "Returns true if a given dependency is in the project's classpath, or false otherwise."
  [^String dep-name]
  (let [pattern (re-pattern (str "/" dep-name ".*\\.jar$"))]
    (->> (classpath/classpath)
         (map #(.getPath %))
         (some (partial re-find pattern))
         boolean)))
