(ns midje-nrepl.coverage
  (:require [cloverage.coverage :as coverage]
            [cloverage.dependency :as dependency]
            [cloverage.instrument :as instrument]
            [cloverage.report :as report]
            [midje-nrepl.misc :as misc]
            [midje-nrepl.project-info :as project-info]))

(defn- summarize-coverage [forms threshold]
  (let [{:keys [percent-forms-covered percent-lines-covered] :as totals} (report/total-stats forms)
        percent-covered                                                  (apply min (vals totals))]
    {:percent-of-forms   (misc/percent percent-forms-covered)
     :percent-of-lines   (misc/percent percent-lines-covered)
     :coverage-threshold threshold
     :result             (if (<= percent-covered threshold)
                           :low-coverage
                           :acceptable-coverage)}))

(defn- instrument-namespaces [namespaces]
  (let [ordered-namespaces (dependency/in-dependency-order namespaces)]
    (doseq [namespace ordered-namespaces]
      (binding [coverage/*instrumented-ns* namespace]
        (instrument/instrument #'coverage/track-coverage namespace)))))

(defn code-coverage [runner]
  (fn [options]
    (let [{:keys [coverage-threshold source-namespaces]
           :or   {coverage-threshold 50
                  source-namespaces  (project-info/find-namespaces-in (project-info/get-source-paths))}} options]
      (binding [coverage/*covered* (atom [])]
        (instrument-namespaces source-namespaces)
        (let [report-map (runner options)
              forms      (report/gather-stats @coverage/*covered*)]
          (assoc report-map
                 :coverage {:summary (summarize-coverage forms coverage-threshold)}))))))
