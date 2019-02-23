(ns midje-nrepl.middleware.test-info-test
  (:require [matcher-combinators.midje :refer [match]]
            [midje-nrepl.middleware.test-info :as test-info]
            [midje-nrepl.project-info :as project-info]
            [midje.sweet :refer :all]
            [nrepl.transport :as transport]))

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
              (project-info/find-namespaces-in ["test"]) => ['octocat.arithmetic-test 'octocat.colls-test]
              (transport/send ..transport.. (match {:test-namespaces ["octocat.arithmetic-test" "octocat.colls-test"]})) => irrelevant
              (transport/send ..transport.. (match {:status #{:done}})) => irrelevant))

       (fact "when the parameter `test-paths` is omitted,
returns a list of all known test namespaces"
             (test-info/handle-test-info {:op        "test-namespaces"
                                          :transport ..transport..})
             => irrelevant
             (provided
              (project-info/get-test-paths) => ["src/clojure/test" "test"]
              (project-info/find-namespaces-in ["src/clojure/test" "test"]) => ['octocat.arithmetic-test 'octocat.colls-test 'octocat.mocks-test]
              (transport/send ..transport.. (match {:test-namespaces ["octocat.arithmetic-test" "octocat.colls-test" "octocat.mocks-test"]})) => irrelevant
              (transport/send ..transport.. (match {:status #{:done}})) => irrelevant)))
