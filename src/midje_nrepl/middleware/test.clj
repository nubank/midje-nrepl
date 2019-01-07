(ns midje-nrepl.middleware.test
  (:require [cider.nrepl.middleware.stacktrace :as stacktrace]
            [clojure.tools.nrepl.misc :refer [response-for]]
            [clojure.tools.nrepl.transport :as transport]
            [midje-nrepl.misc :as misc]
            [midje-nrepl.test-runner :as test-runner]
            [orchard.misc :refer [transform-value]]))

(defn- send-report [{:keys [transport] :as message} report]
  (transport/send transport (response-for message (transform-value report))))

(defn- test-all-reply [message]
  (let [strings->regexes #(map re-pattern %)
        options          (misc/parse-options message {:test-paths    identity
                                                      :ns-exclusions strings->regexes
                                                      :ns-inclusions strings->regexes})
        report           (test-runner/run-all-tests options)]
    (send-report message report)))

(defn- test-ns-reply [{:keys [ns] :as message}]
  (let [namespace (symbol ns)
        report    (test-runner/run-tests-in-ns namespace)]
    (send-report message report)))

(defn- test-reply [{:keys [ns line source] :or {line 1} :as message}]
  (let [namespace (symbol ns)
        report    (test-runner/run-test namespace source line)]
    (send-report message report)))

(defn- retest-reply [message]
  (->> (test-runner/re-run-non-passing-tests)
       (send-report message)))

(defn- test-stacktrace-reply [{:keys [index ns print-fn transport] :as message}]
  (let [namespace (symbol ns)
        exception (test-runner/get-exception-at namespace index)]
    (if exception
      (doseq [cause (stacktrace/analyze-causes exception print-fn)]
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
