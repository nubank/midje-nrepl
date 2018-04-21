(ns midje-nrepl.middleware.test-test
  (:require [clojure.java.io :as io]
            [clojure.tools.nrepl.transport :as transport]
            [matcher-combinators.midje :refer [match]]
            [midje-nrepl.middleware.test :as test]
            [midje-nrepl.test-runner :as test-runner]
            [midje.sweet :refer :all]
            [orchard.misc :as misc]))

(def report {:results
             {'octocat.arithmetic-test
              [{:context  ["about arithmetic operations" "this is a crazy arithmetic"]
                :ns       'octocat.arithmetic-test
                :file     (io/file "file:/home/john-doe/dev/projects/octocat/test/octocat/arithmetic_test.clj")
                :expected 6
                :actual   5
                :message  '()
                :type     :fail}]}
             :summary    {:error 0 :fail 1 :ns 1 :pass 0 :skip 0 :test 1}
             :testing-ns 'octocat.arithmetic-test})

(def transformed-report (misc/transform-value report))

(facts "about handling test operations"
       (against-background
        (transport/send ..transport.. transformed-report) => irrelevant
        (transport/send ..transport.. (match {:status #{:done}})) => irrelevant)

       (fact "when the op is `midje-test-ns`, it runs all tests in the given namespace"
             (test/handle-test {:ns "octocat.arithmetic-test" :op "midje-test-ns" :transport ..transport..}) => irrelevant
             (provided
              (test-runner/run-tests-in-ns 'octocat.arithmetic-test) => report))

       (fact "when the op is `midje-test`, it runs the given test"
             (test/handle-test {:ns "octocat.arithmetic-test"
                                :op "midje-test"
                                :test-forms "(fact (+ 2 3) => 6)"
                                :transport ..transport..}) => irrelevant
             (provided
              (test-runner/run-test 'octocat.arithmetic-test
                                    "(fact (+ 2 3) => 6)") => report))

       (fact "when the op is `midje-retest`, it re-runs the last failed tests"
             (test/handle-test {:op "midje-retest" :transport ..transport..}) => irrelevant
             (provided
              (test-runner/re-run-failed-tests) => report))

       (fact "when the op doesn't match none of the supported ops, it does nothing"
             (test/handle-test {:op "eval"}) => irrelevant
             (provided
              (transport/send anything anything) => irrelevant :times 0)))