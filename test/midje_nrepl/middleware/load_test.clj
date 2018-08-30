(ns midje-nrepl.middleware.load-test
  (:require [midje-nrepl.middleware.load :as load]
            [matcher-combinators.midje :refer [match]]
            [midje-nrepl.reporter :as reporter :refer [with-in-memory-reporter]]
            [midje.sweet :refer :all]))

(defn load-test-namespace-handler [message]
  (with-in-memory-reporter 'octocat.arithmetic-test
    (require 'octocat.arithmetic-test :reload))
  (assoc message ::loaded? true))

(facts "about the load middleware"

       (fact "the dummy handler above loads a given test namespace and assoc's a key into the message map"
             (load-test-namespace-handler {:op "load"})
             => (match {:op       "load"
                        ::loaded? true}))

       (fact "now as tests were executed, the reporter has some test results"
             (reporter/has-test-results?) => true)

       (fact "the load handler delegates the incoming message to the higher handler as expected"
             (load/handle-load {:op "load"} load-test-namespace-handler)
             => (match {:op       "load"
                        ::loaded? true}))

       (fact "as the handle-load function binds `clojure.test/*load-tests*` to false, Midje doesn't check the facts when the namespace is loaded,
so the reporter has no test results"
             (reporter/has-test-results?) => false
             ))
