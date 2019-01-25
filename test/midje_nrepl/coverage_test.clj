(ns midje-nrepl.coverage-test
  (:require [matcher-combinators.midje :refer [match]]
            [midje-nrepl.coverage :as coverage]
            [midje-nrepl.runner :as runner]
            [midje.sweet :refer :all]))

(facts "about code coverage"

       (fact "runs tests by collecting coverage information"
             ((coverage/code-coverage runner/run-tests-in-ns) {:ns 'midje-nrepl.middleware.version-test
                                                               :source-namespaces ['midje-nrepl.middleware.version]})
             => (match {:coverage {:summary {:percent-of-forms "100%"
                                             :percent-of-lines "100%"
                                             :coverage-threshold 50
                                             :result :acceptable-coverage}}})))
