(ns midje-nrepl.test-runner
  (:require [midje-nrepl.reporter :refer [with-reporter-for]])
  (:import clojure.lang.Symbol))

(def ^:private test-results (atom nil))

(defmacro ^:private keeping-test-results [& forms]
  `(let [report# ~@forms]
     (reset! test-results (report# :results))
     report#))

(defn- run-test*
  [^Symbol namespace ^String test-forms]
  (with-reporter-for namespace
    (binding [*ns* (the-ns namespace)]
      (eval (read-string test-forms)))))

(defn run-test
  [^Symbol namespace ^String test-forms]
  (keeping-test-results
   (run-test* namespace test-forms)))

(defn- run-tests-in-ns*
  [^Symbol namespace]
  (with-reporter-for namespace
    (require namespace :reload)))

(defn run-tests-in-ns
  [^Symbol namespace]
  (keeping-test-results
   (run-tests-in-ns* namespace)))

(defn- merge-reports [a-report other]
  {:results (merge (:results a-report) (:results other))
   :summary (merge-with + (:summary a-report) (:summary other))})

(defn run-all-tests
  []
  (keeping-test-results
   (let [not=zero (complement zero?)]
     (->> ['octocat.arithmetic-test 'octocat.colls-test 'octocat.mocks-test 'clojure.core]
          (map run-tests-in-ns*)
          (filter #(-> % :summary :test not=zero))
          (reduce merge-reports)))))

(defn- failed-tests []
  (->> @test-results
       vals
       flatten
       (filter #(#{:error :fail} (:type %)))))

(defn re-run-failed-tests
  []
  (keeping-test-results
   (->> (failed-tests)
        (map (fn [{:keys [ns test-forms]}]
               (run-test* ns test-forms)))
        (reduce merge-reports))))
