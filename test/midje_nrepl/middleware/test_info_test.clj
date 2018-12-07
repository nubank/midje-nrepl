(ns midje-nrepl.middleware.test-info-test
  (:require [clojure.tools.nrepl.transport :as transport]
            [matcher-combinators.midje :refer [match]]
            [midje-nrepl.middleware.test-info :as test-info]
            [midje-nrepl.project-info :as project-info]
            [midje.sweet :refer :all]))

(facts "about getting information about test paths and namespaces"

       (fact "returns the project's test paths"
             (test-info/handle-test-info {:op        "test-paths"
                                          :transport ..transport..})
             => irrelevant
             (provided
              (project-info/get-test-paths) => ["test" "integration"]
              (transport/send ..transport.. (match {:test-paths ["test" "integration"]})) => irrelevant
              (transport/send ..transport.. (match {:status #{:done}})) => irrelevant))

       (fact "returns the test namespaces declared within a given test path"
             (test-info/handle-test-info {:op         "test-namespaces"
                                          :transport  ..transport..
                                          :test-paths ["test"]})
             => irrelevant
             (provided
              (project-info/get-test-namespaces-in ["test"]) => ['octocat.arithmetic-test 'octocat.colls-test]
              (transport/send ..transport.. (match {:test-namespaces ["octocat.arithmetic-test" "octocat.colls-test"]})) => irrelevant
              (transport/send ..transport.. (match {:status #{:done}})) => irrelevant)))
