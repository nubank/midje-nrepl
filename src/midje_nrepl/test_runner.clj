(ns midje-nrepl.test-runner
  (:require [midje-nrepl.reporter :refer [with-reporter-for]])
  (:import clojure.lang.Symbol))

(defn run-tests-in-ns
  [^Symbol namespace]
  (with-reporter-for namespace
    (require namespace :reload)))

(defn- merge-reports [a-report other]
  {:results (merge (:results a-report) (:results other))
   :summary (merge-with + (:summary a-report) (:summary other))})

(defn run-all-tests
  []
  (let [not=zero (complement zero?)]
    (->> ['octocat.arithmetic-test 'octocat.colls-test 'octocat.mocks-test 'clojure.core]
         (map run-tests-in-ns)
         (filter #(-> % :summary :test not=zero))
         (reduce merge-reports))))
