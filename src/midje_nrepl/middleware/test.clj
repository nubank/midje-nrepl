(ns midje-nrepl.middleware.test
  (:require [clojure.tools.nrepl.misc :refer [response-for]]
            [clojure.tools.nrepl.transport :as transport]
            [midje-nrepl.test-runner :as test-runner]
            [orchard.misc :as misc]))

(defn- handle-test-ns-op
  [{:keys [ns transport] :as message}]
  (let [namespace (symbol ns)
        report    (test-runner/run-tests-in-ns namespace)]
    (transport/send transport (response-for message (misc/transform-value report)))
    (transport/send transport (response-for message :status :done))))

(defn- handle-test-op
  [{:keys [ns test-forms transport] :as message}]
  (let [namespace (symbol ns)
        report    (test-runner/run-test namespace test-forms)]
    (transport/send transport (response-for message (misc/transform-value report)))
    (transport/send transport (response-for message :status :done))))

(defn- handle-retest-op
  [{:keys [transport] :as message}]
  (let [report (test-runner/re-run-failed-tests)]
    (transport/send transport (response-for message (misc/transform-value report)))
    (transport/send transport (response-for message :status :done))))

(def ^:private handlers-map
  {"midje-test-ns" handle-test-ns-op
   "midje-test"    handle-test-op
   "midje-retest"  handle-retest-op})

(defn handle-test
  [{:keys [op] :as message}]
  (-> (get handlers-map op identity)
      (apply [message])))
