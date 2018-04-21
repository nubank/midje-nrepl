(ns midje-nrepl.middleware.version
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.nrepl.misc :refer [response-for]]
            [clojure.tools.nrepl.transport :as transport]))

(def ^:private project-clj "META-INF/leiningen/midje-nrepl/midje-nrepl/project.clj")

(def ^:private version-regex #"^(\d+).(\d+).(\d+)-?(.*)")

(defn get-current-version
  "Returns the current version of midje-nrepl."
  []
  (-> (io/resource project-clj)
      slurp
      read-string
      (nth 2)))

(defn version-info [^String version]
  (->> (re-matches version-regex version)
       (remove string/blank?)
       (zipmap [:version-string :major :minor :incremental :qualifier])))

(defn handle-version
  "Handles the `midje-nrepl-version` op, by returning information of midje-nrepl's current version."
  [{:keys [transport] :as message}]
  (transport/send transport
                  (response-for message :status :done
                                :midje-nrepl (version-info (get-current-version)))))
