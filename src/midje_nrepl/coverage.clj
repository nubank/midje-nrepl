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

(defn- assoc-coverage-stats [report-map coverage-threshold]
  (let [forms (report/gather-stats @coverage/*covered*)]
    (assoc report-map
           :coverage {:summary (summarize-coverage forms coverage-threshold)})))

(defn- instrument-namespace [namespace logger]
  (binding [coverage/*instrumented-ns* namespace]
    (logger :info "Instrumenting %s..." namespace)
    (try
      (instrument/instrument #'coverage/track-coverage namespace)
      (coverage/mark-loaded namespace)
      :success
      (catch Exception e
        (logger :error "Could not instrument namespace %s. %s." namespace (str e))
        :error))))

(defn- try-to-instrument-namespaces [namespaces logger]
  (let [results                      (map #(instrument-namespace % logger) namespaces)
        {:keys [success error]
         :or   {success 0, error 0}} (frequencies results)]
    (if (zero? error)
      (do (logger :info "All namespaces (%d) were successfully instrumented." (+ success error))
          :success)
      (do (logger :error "Could not capture code coverage due to previous problems.")
          :error))))

(defn instrument-namespaces [namespaces logger]
  (logger :info "Loading namespaces...")
  (let [ordered-namespaces (dependency/in-dependency-order namespaces)]
    (if (seq ordered-namespaces)
      (try-to-instrument-namespaces ordered-namespaces logger)
      (do (logger :error "Could not instrument namespaces: there is a cyclic dependency.")
          :error))))

(defn wrap-logger [coverage-logger]
  (fn [level message & args]
    (coverage-logger level (apply format message args))))

(defn code-coverage [runner]
  (fn [options]
    (let [{:keys [coverage-threshold coverage-logger source-namespaces]
           :or   {coverage-threshold 50
                  coverage-logger    (constantly nil)
                  source-namespaces  (project-info/find-namespaces-in (project-info/get-source-paths))}} options
          logger                                                                                         (wrap-logger coverage-logger)]
      (binding [coverage/*covered* (atom [])]
        (let [instrumentation-result (instrument-namespaces source-namespaces logger)
              report-map             (runner options)]
          (if (= instrumentation-result :success)
            (assoc-coverage-stats report-map coverage-threshold)
            report-map))))))
