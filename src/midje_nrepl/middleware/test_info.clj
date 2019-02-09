(ns midje-nrepl.middleware.test-info
  (:require [midje-nrepl.project-info :as project-info]
            [nrepl.misc :refer [response-for]]
            [nrepl.transport :as transport]
            [orchard.misc :as misc]))

(defn- test-namespaces-reply [{:keys [transport test-paths] :as message}]
  (let [test-paths      (or test-paths (project-info/get-test-paths))
        test-namespaces (project-info/find-namespaces-in test-paths)]
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
