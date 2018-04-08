(ns midje-nrepl.leiningen-plugin
  (:require [midje-nrepl.nrepl :as midje-nrepl]
            [midje-nrepl.middlewares.version :as version]))

(defn middleware [project]
  (-> project
      (update :dependencies (fnil concat [])
              [['midje-nrepl (version/get-current-version)]])
      (update-in [:repl-options :nrepl-middleware]                 (fnil concat [])
                 midje-nrepl/middlewares)))
