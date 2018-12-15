(ns integration.test-info
  (:require [integration.helpers :refer [send-message]]
            [matcher-combinators.midje :refer [match]]
            [midje.sweet :refer :all]))

(facts "about getting information about test paths and namespaces"

       (fact "gets all known test paths in the project"
             (send-message {:op "test-paths"})
             => (match [{:test-paths ["integration" "test"]}
                        {:status ["done"]}]))

       (fact "gets all test namespaces defined in the project"
             (send-message {:op "test-namespaces"})
             => (match [{:test-namespaces ["integration.database-test"
                                           "octocat.arithmetic-test"
                                           "octocat.side-effects-test"]}
                        {:status ["done"]}]))

       (tabular (fact "by sending the parameter `test-paths`, clients can find namespaces only in the mentioned test paths"
                      (send-message {:op         "test-namespaces"
                                     :test-paths ?test-paths})
                      => (match [{:test-namespaces ?namespaces}
                                 {:status ["done"]}]))
                ?test-paths                                                                         ?namespaces
                ["test"]                             ["octocat.arithmetic-test" "octocat.side-effects-test"]
                ["integration"]                                                       ["integration.database-test"]
                ["integration" "test"] ["integration.database-test" "octocat.arithmetic-test" "octocat.side-effects-test"]))
