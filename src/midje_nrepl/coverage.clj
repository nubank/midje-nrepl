(ns midje-nrepl.coverage
  (:require [cloverage.coverage :as coverage]
            [cloverage.debug :as debug]
            [cloverage.dependency :as dependency]
            [cloverage.instrument :as instrument]
            [cloverage.report :as report]
            [cloverage.report.console :as console]
            [midje-nrepl.project-info :as project-info]))

(defn- summarize-coverage [forms threshold]
  (let [totals          (report/total-stats forms)
        percent-covered (apply min (vals totals))
        percent-value   #(format "%.2f%%" %)]
    (-> totals
        (update :percent-forms-covered percent-value)
        (update :percent-lines-covered percent-value)
        (assoc :threshold  threshold)
        (assoc :status (if (<= percent-covered threshold)
                         :low-coverage
                         :acceptable-coverage)))))

(defn- instrument-namespaces [namespaces]
  (let [ordered-namespaces (dependency/in-dependency-order namespaces)]
    (doseq [namespace ordered-namespaces]
      (binding [coverage/*instrumented-ns* namespace]
        (instrument/instrument #'coverage/track-coverage namespace)))))

(defn covering [options runner-fn]
  (let [{:keys [debug? threshold source-namespaces]
         :or   {debug?            false
                threshold         50
                source-namespaces (project-info/find-namespaces-in (project-info/get-source-paths))}} (options :coverage)]
    (binding [coverage/*covered* (atom [])
              debug/*debug*      debug?]
      (instrument-namespaces source-namespaces)
      (let [report-map (runner-fn)
            forms      (report/gather-stats @coverage/*covered*)]
        (assoc report-map
               :coverage (summarize-coverage forms threshold))))))
