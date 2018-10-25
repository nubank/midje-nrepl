(ns midje-nrepl.plugin
  (:require [midje-nrepl.middleware.version :as version]
            [midje-nrepl.nrepl :as midje-nrepl]))

(def ^:private clojure-tools-namespace ['org.clojure/tools.namespace "0.3.0-alpha4"])

(defn- remove-conflicting-dependencies [dependencies]
  (remove (fn [[name]]
            (= name 'org.clojure/tools.namespace))
          dependencies))

(defn middleware [project]
  (-> project
      (update :dependencies (fnil concat [])
              [['midje-nrepl (version/get-current-version)]])
      (update :dependencies remove-conflicting-dependencies)
      (update :dependencies concat [clojure-tools-namespace])
      (update-in [:repl-options :nrepl-middleware]                 (fnil concat [])
                 midje-nrepl/middleware)))
