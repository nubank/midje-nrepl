(ns midje-nrepl.middleware.test-test
  (:require [clojure.java.io :as io]
            [clojure.tools.nrepl.transport :as transport]
            [matcher-combinators.midje :refer [match]]
            [midje-nrepl.middleware.test :as test]
            [midje-nrepl.test-runner :as test-runner]
            [midje.sweet :refer :all]
            [orchard.misc :as misc]))

(def test-report {:results
                  {'octocat.arithmetic-test
                   [{:context  ["about arithmetic operations" "this is a crazy arithmetic"]
                     :ns       'octocat.arithmetic-test
                     :file     (io/file "/home/john-doe/dev/projects/octocat/test/octocat/arithmetic_test.clj")
                     :index    0
                     :expected 6
                     :actual   5
                     :message  '()
                     :type     :fail}]}
                  :summary {:error 0 :fact 1 :fail 1 :ns 1 :pass 0 :skip 0 :test 1}})

(def transformed-report (misc/transform-value test-report))

(def exception (RuntimeException. "An unexpected error was thrown" (ArithmeticException. "Divid by zero")))

(facts "about handling test operations"

       (fact "run all tests in the project and sends the report to the client"
             (test/handle-test {:op        "midje-test-all"
                                :transport ..transport..}) => irrelevant
             (provided
              (test-runner/run-all-tests) => test-report
              (transport/send ..transport.. transformed-report) => irrelevant
              (transport/send ..transport.. (match {:status #{:done}})) => irrelevant))

       (fact "runs all tests in the given namespace and sends the report to the client"
             (test/handle-test {:op        "midje-test-ns"
                                :ns        "octocat.arithmetic-test"
                                :transport ..transport..}) => irrelevant
             (provided
              (test-runner/run-tests-in-ns 'octocat.arithmetic-test) => test-report
              (transport/send ..transport.. transformed-report) => irrelevant
              (transport/send ..transport.. (match {:status #{:done}})) => irrelevant))

       (fact "runs the given test and sends the report to the client"
             (test/handle-test {:op        "midje-test"
                                :ns        "octocat.arithmetic-test"
                                :line      10
                                :source    "(fact (+ 2 3) => 6)"
                                :transport ..transport..}) => irrelevant
             (provided
              (test-runner/run-test 'octocat.arithmetic-test "(fact (+ 2 3) => 6)" 10) => test-report
              (transport/send ..transport.. transformed-report) => irrelevant
              (transport/send ..transport.. (match {:status #{:done}})) => irrelevant))

       (fact "re-runs non-passing tests and sends the report to the client"
             (test/handle-test {:op        "midje-retest"
                                :transport ..transport..}) => irrelevant
             (provided
              (test-runner/re-run-non-passing-tests) => test-report
              (transport/send ..transport.. transformed-report) => irrelevant
              (transport/send ..transport.. (match {:status #{:done}})) => irrelevant))

       (fact "sends the stacktrace of a given erring test to the client"
             (test/handle-test {:op        "midje-test-stacktrace"
                                :ns        "octocat.arithmetic-test"
                                :index     2
                                :print-fn  println
                                :transport ..transport..}) => irrelevant
             (provided
              (test-runner/get-exception-at 'octocat.arithmetic-test 2) => exception
              (transport/send ..transport.. (match {:class      "java.lang.RuntimeException"
                                                    :message    "An unexpected error was thrown"
                                                    :stacktrace (complement empty?)}))  => irrelevant
              (transport/send ..transport.. (match {:class      "java.lang.ArithmeticException"
                                                    :message    "Divid by zero"
                                                    :stacktrace (complement empty?)})) => irrelevant
              (transport/send ..transport.. (match {:status #{:done}})) => irrelevant))

       (fact "sends a :no-test-stacktrace status when there is no stacktrace to be returned at the given position"
             (test/handle-test {:op        "midje-test-stacktrace"
                                :ns        "octocat.arithmetic-test"
                                :index     0
                                :print-fn  println
                                :transport ..transport..}) => irrelevant
             (provided
              (test-runner/get-exception-at 'octocat.arithmetic-test 0) => nil
              (transport/send ..transport.. (match {:status #{:no-stacktrace}})) => irrelevant
              (transport/send ..transport.. (match {:status #{:done}})) => irrelevant)))
