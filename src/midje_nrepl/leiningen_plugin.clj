(ns midje-nrepl.leiningen-plugin
  (:require [midje-nrepl.nrepl :as midje-nrepl]))

(defn middleware [project]
  (update-in project [:repl-options :nrepl-middleware]
             (fnil concat []) (map resolve midje-nrepl/middlewares)))
