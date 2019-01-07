(ns midje-nrepl.project-info
  (:require [clojure.java.io :as io]
            [clojure.tools.namespace.find :as namespace.find]
            [orchard.namespace :as namespace])
  (:import [java.io FileReader PushbackReader]))

(def ^:private leiningen-project-file "project.clj")

(defn file-for-ns [namespace]
  (some-> (name namespace)
          namespace/ns-path
          io/file))

(defn- project-working-dir []
  (.getCanonicalFile (io/file ".")))

(defn read-leiningen-project []
  (let [project-file (io/file (project-working-dir) leiningen-project-file)]
    (with-open [reader (PushbackReader. (FileReader. project-file))]
      (read reader))))

(defn read-project-map []
  (let [[_ project-name version & others] (read-leiningen-project)]
    (into {:name    project-name
           :version version}
          (apply hash-map others))))

(defn existing-dir? [candidate]
  (.isDirectory (io/file (project-working-dir) candidate)))

(defn- get-project-paths [key default]
  (let [project-map (read-project-map)]
    (->> (get project-map key [default])
         (filter existing-dir?)
         sort)))

(def get-source-paths
  #(get-project-paths :source-paths "src"))

(def get-test-paths
  #(get-project-paths :test-paths "test"))

(defn find-namespaces-in [paths]
  (->> paths
       (map (partial io/file (project-working-dir)))
       (mapcat namespace.find/find-namespaces-in-dir)
       sort))
