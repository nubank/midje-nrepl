(ns midje-nrepl.test-runner
  (:require [midje-nrepl.reporter :as reporter :refer [with-in-memory-reporter]])
  (:import clojure.lang.Symbol))

(def test-results (atom {}))

(defn get-exception-at [^Symbol ns ^Integer index]
  {:pre [(symbol? ns) (or (zero? index) (pos-int? index))]}
  (get-in @test-results [ns index :error]))

(defmacro ^:private caching-test-results [& forms]
  `(let [report# ~@forms]
     (reset! test-results (report# :results))
     report#))

(defn test-forms
  [^Symbol namespace & forms]
  (with-in-memory-reporter namespace
    (binding [*ns* (the-ns namespace)]
      (->> forms
           (apply list)
           (cons 'do)
           eval))))

(defn run-test
  [^Symbol namespace ^String forms]
  {:pre [(symbol? namespace) (string? forms)]}
  (caching-test-results
   (test-forms namespace (read-string forms))))

(defn- run-tests-in-ns*
  [^Symbol namespace]
  (with-in-memory-reporter namespace
    (require namespace :reload)))

(defn run-tests-in-ns
  "Runs Midje tests in the given namespace.
   Returns the test report."
  [^Symbol namespace]
  {:pre [(symbol? namespace)]}
  (caching-test-results
   (run-tests-in-ns* namespace)))

(defn- merge-reports [reports]
  (reduce (fn [a b]
            {:results (merge (:results a) (:results b))
             :summary (merge-with + (:summary a) (:summary b))})
          reporter/no-tests reports))

(defn- failed-test-forms [results]
  (->> results
       (filter #(#{:error :fail} (:type %)))
       distinct
       (map (comp read-string :test-forms))))

(defn re-run-failed-tests
  "Re-runs tests that have failed in the last execution.
  Returns the test report."
  []
  (caching-test-results
   (->> @test-results
        (map #(->> (second %) failed-test-forms (cons (first %))))
        (remove #(= 1 (count %)))
        (map (partial apply test-forms))
        merge-reports)))
