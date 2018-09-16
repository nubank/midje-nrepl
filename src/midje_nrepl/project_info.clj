(ns midje-nrepl.project-info
  (:require [clojure.java.io :as io]
            [clojure.tools.namespace.find :as namespace.find])
  (:import (java.io FileReader PushbackReader)))

(def ^:private leiningen-project-file "project.clj")

(defn- current-working-dir []
  (.getCanonicalFile (io/file ".")))

(defn- read-test-paths []
  (let [cwd          (current-working-dir)
        project-file (io/file cwd leiningen-project-file)]
    (with-open [reader (PushbackReader. (FileReader. project-file))]
      (->> (read reader)
           (drop-while #(not (keyword? %)))
           (apply hash-map)
           :test-paths
           (map #(hash-map % (io/file cwd %)))
           (apply merge)))))

(defn find-test-dirs []
  (keys (read-test-paths)))

(defn find-test-namespaces
  ([]
   (find-test-namespaces (find-test-dirs)))
  ([test-dirs]
   (let [test-paths (read-test-paths)]
     (->> test-dirs
          (map #(get test-paths %))
          (mapcat namespace.find/find-namespaces-in-dir)))))
