(ns midje-nrepl.plugin
  (:require [leiningen.core.main :as lein]
            [midje-nrepl.middleware.version :as version]
            [midje-nrepl.nrepl :as midje-nrepl]))

(def ^:private min-lein-version "2.8.3")

(def ^:private clojure-tools-namespace ['org.clojure/tools.namespace "0.3.0-alpha4" :exclusions ['org.clojure/tools.reader]])

(defn- lein-satisfies-min-required-version? []
  (lein/version-satisfies? (lein/leiningen-version) min-lein-version))

(defn- midje-nrepl-wont-be-included-in-your-project []
  (lein/warn (format "Warning: midje-nrepl requires Leiningen %s or greater" min-lein-version))
  (lein/warn "Warning: midje-nrepl won't be included in your project"))

(defn- remove-conflicting-dependencies [dependencies]
  (remove (fn [[name]]
            (= name 'org.clojure/tools.namespace))
          dependencies))

(defn- include-midje-nrepl-in [project]
  (-> project
      (update :dependencies (fnil concat [])
              [['nubank/midje-nrepl (version/get-current-version)]])
      (update :dependencies remove-conflicting-dependencies)
      (update :dependencies concat [clojure-tools-namespace])
      (update-in [:repl-options :nrepl-middleware]                 (fnil concat [])
                 midje-nrepl/middleware)))

(defn middleware [project]
  (if (lein-satisfies-min-required-version?)
    (include-midje-nrepl-in project)
    (do (midje-nrepl-wont-be-included-in-your-project)
        project)))
