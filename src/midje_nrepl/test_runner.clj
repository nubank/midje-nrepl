(ns midje-nrepl.test-runner
  (:require [midje-nrepl.reporter :refer [with-reporter-for]])
  (:import clojure.lang.Symbol))

(defn run-tests-from-ns
  [^Symbol namespace]
  (with-reporter-for namespace
    (require namespace :reload)))

(defn run-all-tests
  []
  )
