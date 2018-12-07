(ns midje-nrepl.middleware.test-info
  (:require [clojure.tools.nrepl.misc :refer [response-for]]
            [clojure.tools.nrepl.transport :as transport]
            [midje-nrepl.project-info :as project-info]
            [orchard.misc :as misc]))

(defn- test-namespaces-reply [{:keys [transport test-paths] :as message}]
  (let [test-namespaces (project-info/get-test-namespaces-in test-paths)]
    (transport/send transport
                    (response-for message :test-namespaces (misc/transform-value test-namespaces)))))

(defn- test-paths-reply [{:keys [transport] :as message}]
  (transport/send transport
                  (response-for message :test-paths (project-info/get-test-paths))))

(defn handle-test-info [{:keys [op transport] :as message}]
  (case op
    "test-paths"      (test-paths-reply message)
    "test-namespaces" (test-namespaces-reply message))
  (transport/send transport (response-for message :status :done)))
