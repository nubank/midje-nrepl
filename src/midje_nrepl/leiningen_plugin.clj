(ns midje-nrepl.leiningen-plugin
  (:require [midje-nrepl.nrepl :as midje-nrepl]))

(defn middleware [project]
  (-> project
      (update :dependencies (fnil concat [])
              '(              [midje-nrepl "0.1.0-SNAPSHOT"]))
      (update-in [:repl-options :nrepl-middleware]                 (fnil concat [])
                 midje-nrepl/middlewares)))
