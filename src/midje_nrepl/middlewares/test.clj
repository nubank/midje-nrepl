(ns midje-nrepl.middlewares.test
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

(def ^:private handlers-map
  {"midje-test-ns" handle-test-ns-op})

(defn handle-test
  [{:keys [op] :as message}]
  (-> (get handlers-map op identity)
      (apply [message])))
