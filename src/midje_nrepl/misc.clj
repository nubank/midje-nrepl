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
