(ns midje-nrepl.coverage
  (:require [cloverage.coverage :as coverage]
            [cloverage.dependency :as dependency]
            [cloverage.instrument :as instrument]
            [cloverage.report :as report]
            [midje-nrepl.misc :as misc]
            [midje-nrepl.project-info :as project-info]))

(defn- coverage-result [percent-covered threshold]
  (if (<= percent-covered threshold)
    :low-coverage
    :good-coverage))

(defn- stats-per-ns [forms threshold]
  (letfn [(percent-value [covered total]
            (if (zero? covered)
              0
              (float (/ (* covered 100) total))))]
    (->> forms
         report/file-stats
         (sort-by :lib)
         (map (fn [{:keys [lib covered-forms forms covered-lines instrd-lines partial-lines]}]
                (let [covered-lines    (+ covered-lines partial-lines)
                      percent-of-forms (percent-value covered-forms forms)
                      percent-of-lines (percent-value covered-lines instrd-lines)]
                  {:ns     lib
                   :forms  {:total forms :covered covered-forms :percent-value (misc/percent percent-of-forms)}
                   :lines  {:covered covered-lines :total instrd-lines :percent-value (misc/percent percent-of-lines)}
                   :result (coverage-result (min percent-of-forms percent-of-lines) threshold)}))))))

(defn- summarize-coverage [forms threshold]
  (let [{:keys [percent-forms-covered percent-lines-covered] :as totals} (report/total-stats forms)
        percent-covered                                                  (apply min (vals totals))]
    {:percent-of-forms   (misc/percent percent-forms-covered)
     :percent-of-lines   (misc/percent percent-lines-covered)
     :coverage-threshold threshold
     :result             (coverage-result percent-covered threshold)}))

(defn- assoc-coverage-stats [report-map coverage-threshold]
  (let [forms (report/gather-stats @coverage/*covered*)]
    (assoc report-map
           :coverage {:namespaces (stats-per-ns forms coverage-threshold)
                      :summary    (summarize-coverage forms coverage-threshold)})))

(defn- instrument-namespace [namespace]
  (binding [coverage/*instrumented-ns* namespace]
    (instrument/instrument #'coverage/track-coverage namespace)
    (coverage/mark-loaded namespace)))

(defn- try-to-instrument-namespace [namespace logger]
  (try
    (logger :info "Instrumenting %s..." namespace)
    (instrument-namespace namespace)
    :success
    (catch Exception e
      (logger :error "Could not instrument namespace %s. %s." namespace (str e))
      :error)))

(defn- try-to-instrument-namespaces [namespaces logger]
  (let [results                      (map #(try-to-instrument-namespace % logger) namespaces)
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
    (let [{:keys [threshold logger source-namespaces]
           :or   {threshold         50
                  logger            (constantly nil)
                  source-namespaces (project-info/find-namespaces-in (project-info/get-source-paths))}} (:coverage options)
          coverage-logger                                                                                (wrap-logger logger)]
      (binding [coverage/*covered* (atom [])]
        (let [instrumentation-result (instrument-namespaces source-namespaces coverage-logger)
              report-map             (runner options)]
          (if (= instrumentation-result :success)
            (assoc-coverage-stats report-map threshold)
            report-map))))))
