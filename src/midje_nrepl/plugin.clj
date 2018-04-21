(ns midje-nrepl.plugin
  (:require [midje-nrepl.middleware.version :as version]
            [midje-nrepl.nrepl :as midje-nrepl]))

(defn middleware [project]
  (-> project
      (update :dependencies (fnil concat [])
              [['midje-nrepl (version/get-current-version)]])
      (update-in [:repl-options :nrepl-middleware]                 (fnil concat [])
                 midje-nrepl/middleware)))
