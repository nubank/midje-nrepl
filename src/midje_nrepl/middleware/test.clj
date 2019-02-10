(ns midje-nrepl.middleware.test
  (:require [cider.nrepl.middleware.stacktrace :as stacktrace]
            [midje-nrepl.misc :as misc]
            [midje-nrepl.profiler :as profiler]
            [midje-nrepl.runner :as runner]
            [nrepl.misc :refer [response-for]]
            [nrepl.transport :as transport]
            [orchard.misc :refer [transform-value]]))

(defmethod transform-value java.time.Duration [duration]
  (profiler/duration->string duration))

(defn- send-report [{:keys [transport] :as message} report]
  (transport/send transport (response-for message (transform-value report))))

(defn- test-all-reply [message]
  (let [strings->regexes  (partial map re-pattern)
        strings->keywords (partial map keyword)
        options           (misc/parse-options message {:ns-exclusions   strings->regexes
                                                       :ns-inclusions   strings->regexes
                                                       :test-exclusions strings->keywords
                                                       :test-inclusions strings->keywords
                                                       :profile?        #(Boolean/parseBoolean %)
                                                       :slowest-tests   int
                                                       :test-paths      identity})
        report            ((profiler/profile runner/run-all-tests) options)]
    (send-report message report)))

(defn- test-ns-reply [message]
  (let [options (misc/parse-options message {:ns symbol})
        report  ((profiler/profile runner/run-tests-in-ns) options)]
    (send-report message report)))

(defn- test-reply [message]
  (let [options (misc/parse-options message {:ns     symbol
                                             :source identity
                                             :line   int})
        report  ((profiler/profile runner/run-test) options)]
    (send-report message report)))

(defn- retest-reply [message]
  (->> ((profiler/profile runner/re-run-non-passing-tests) {})
       (send-report message)))

(defn- test-stacktrace-reply [{:keys [index ns print-fn print-options transport] :as message}]
  (let [namespace (symbol ns)
        exception (runner/get-exception-at namespace index)]
    (if exception
      (doseq [cause (stacktrace/analyze-causes exception print-fn print-options)]
        (transport/send transport (response-for message cause)))
      (transport/send transport (response-for message :status :no-stacktrace)))))

(defn handle-test [{:keys [op transport] :as message}]
  (case op
    "midje-test-all"        (test-all-reply message)
    "midje-test-ns"         (test-ns-reply message)
    "midje-test"            (test-reply message)
    "midje-retest"          (retest-reply message)
    "midje-test-stacktrace" (test-stacktrace-reply message))
  (transport/send transport (response-for message :status :done)))
