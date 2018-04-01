(ns midje-nrepl.test-runner
  (:require [midje-nrepl.reporter :refer [with-reporter-for]])
  (:import clojure.lang.Symbol))

(def ^:private test-results (atom {}))

(defmacro ^:private keeping-test-results [& forms]
  `(let [report# ~@forms]
     (reset! test-results (report# :results))
     report#))

(defn- run-tests
  [namespace & test-forms]
  (with-reporter-for namespace
    (binding [*ns* (the-ns namespace)]
      (->> (map read-string test-forms)
           (apply list)
           (cons 'do)
           eval))))

(defn run-test
  [^Symbol namespace ^String test-forms]
  (keeping-test-results
   (run-tests namespace test-forms)))

(defn- run-tests-in-ns*
  [^Symbol namespace]
  (with-reporter-for namespace
    (require namespace :reload)))

(defn run-tests-in-ns
  "Runs Midje tests in the provided namespace.
   Returns the test report."
  [^Symbol namespace]
  (keeping-test-results
   (run-tests-in-ns* namespace)))

(defn- merge-reports [a-report other]
  {:results (merge (:results a-report) (:results other))
   :summary (merge-with + (:summary a-report) (:summary other))})

(defn run-all-tests
  []
  (keeping-test-results
   (->> ['octocat.arithmetic-test 'octocat.colls-test 'octocat.mocks-test 'clojure.core]
        (map run-tests-in-ns*)
        (reduce merge-reports {}))))

(defn- failed-tests [results]
  (->> results
       (filter #(#{:error :fail} (:type %)))
       (map (comp distinct :test-forms))))

(defn re-run-failed-tests
  "Re-runs tests that have failed in the last execution.
  Returns the test report."
  []
  (keeping-test-results
   (->> @test-results
        (map #(->> (second %) failed-tests (cons (first %))))
        (remove #(= 1 (count %)))
        (map (partial apply run-tests))
        (reduce merge-reports {}))))
