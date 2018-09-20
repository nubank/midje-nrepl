(ns midje-nrepl.project-info
  (:require [clojure.java.io :as io]
            [clojure.tools.namespace.find :as namespace.find])
  (:import (java.io FileReader PushbackReader)))

(def ^:private leiningen-project-file "project.clj")

(defn- current-working-dir []
  (.getCanonicalFile (io/file ".")))

(defn- read-leiningen-project []
  (let [project-file (io/file (current-working-dir) leiningen-project-file)]
    (with-open [reader (PushbackReader. (FileReader. project-file))]
      (read reader))))

(defn- read-project-map []
  (->> (read-leiningen-project)
       rest
       (apply hash-map)))

(defn get-test-paths []
  (->> (read-project-map)
       :test-paths
       sort))

(defn get-test-namespaces [test-paths]
  (->> test-paths
       (map (partial io/file (current-working-dir)))
       (mapcat namespace.find/find-namespaces-in-dir)))
