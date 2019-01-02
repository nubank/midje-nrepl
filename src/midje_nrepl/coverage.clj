(ns midje-nrepl.coverage
  (:require [cloverage.coverage :as coverage]
            [cloverage.report :as report]
            [cloverage.report.console :as console]
            [cloverage.instrument :as instrument]
            [cloverage.dependency :as dependency]))

(def coverage-report (atom {}))

(defn- instrument-namespaces [namespaces]
  (let [ordered-namespaces (dependency/in-dependency-order namespaces)]
    (doseq [namespace ordered-namespaces]
      (binding [coverage/*instrumented-ns* namespace]
        (instrument/instrument #'coverage/track-coverage namespace)))))

(defn test-coverage [run-fn]
  (binding [coverage/*covered* (atom [])]
    (instrument-namespaces ['midje-nrepl.formatter])
    (let [report-map (run-fn)
          forms (report/gather-stats @coverage/*covered*)]
      (console/summary forms 50 80)
      report-map)))
