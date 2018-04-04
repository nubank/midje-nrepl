(ns midje-nrepl.test-runner
  (:require [midje-nrepl.reporter :refer [with-reporter-for]])
  (:import clojure.lang.Symbol))

(def test-results (atom {}))

(defmacro ^:private keeping-test-results [& forms]
  `(let [report# ~@forms]
     (reset! test-results (report# :results))
     report#))

(defn test-forms
  [^Symbol namespace & forms]
  (with-reporter-for namespace
    (binding [*ns* (the-ns namespace)]
      (->> forms
           (apply list)
           (cons 'do)
           eval))))

(defn run-test
  [^Symbol namespace ^String forms]
  {:pre [(symbol? namespace) (string? forms)]}
  (keeping-test-results
   (test-forms namespace (read-string forms))))

(defn- run-tests-in-ns*
  [^Symbol namespace]
  (with-reporter-for namespace
    (require namespace :reload)))

(defn run-tests-in-ns
  "Runs Midje tests in the given namespace.
   Returns the test report."
  [^Symbol namespace]
  {:pre [(symbol? namespace)]}
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
        (map (partial apply test-forms))
        (reduce merge-reports {}))))
