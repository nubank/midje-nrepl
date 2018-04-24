(ns midje-nrepl.test-runner-test
  (:require [matcher-combinators.midje :refer [match]]
            [midje-nrepl.test-runner :as test-runner]
            [midje.emission.state :refer [with-isolated-output-counters]]
            [midje.sweet :refer :all]))

(defn actual-file? [candidate]
  (instance? java.io.File candidate))

(def test-forms-report {:results
                        {'midje-nrepl.test-runner-test
                         [{:ns         'midje-nrepl.test-runner-test
                           :file       actual-file?
                           :context    ["(fact 1 => 1)"]
                           :test-forms "(fact 1 => 1)"
                           :type       :pass}
                          {:ns         'midje-nrepl.test-runner-test
                           :file       actual-file?
                           :context    ["(fact (+ 3 2) => 6)"]
                           :test-forms "(fact (+ 3 2) => 6)"
                           :expected   6
                           :actual     5
                           :message    '()
                           :type       :fail}]}
                        :summary    {:ns 1 :fact 2 :fail 1 :error 0 :pass 1 :test 2 :skip 0}
                        :testing-ns 'midje-nrepl.test-runner-test})

(def individual-test-report {:results
                             {'midje-nrepl.test-runner-test
                              [{:ns         'midje-nrepl.test-runner-test
                                :file       actual-file?
                                :context    ["(fact 1 => 1)"]
                                :test-forms "(fact 1 => 1)"
                                :type       :pass}]}
                             :summary    {:ns 1 :fact 1 :fail 0 :error 0 :pass 1 :test 1 :skip 0}
                             :testing-ns 'midje-nrepl.test-runner-test})

(def arithmetic-test-report {:results
                             {'octocat.arithmetic-test
                              [{:context ["about arithmetic operations" "(fact (* 2 5) => 10 :position (pointer.core/line-number-known 9))"]
                                :ns      'octocat.arithmetic-test
                                :file    actual-file?
                                :type    :pass}
                               {:context  ["about arithmetic operations" "this is a crazy arithmetic"]
                                :ns       'octocat.arithmetic-test
                                :file     actual-file?
                                :expected 6
                                :actual   5
                                :message  '()
                                :type     :fail}
                               {:context
                                ["about arithmetic operations" "two assertions in the same fact; the former is correct while the later is wrong"]
                                :ns   'octocat.arithmetic-test
                                :file actual-file?
                                :type :pass}
                               {:context
                                ["about arithmetic operations" "two assertions in the same fact; the former is correct while the later is wrong"]
                                :ns       'octocat.arithmetic-test
                                :file     actual-file?
                                :expected 3
                                :actual   2
                                :message  '()
                                :type     :fail}
                               {:context  ["about arithmetic operations" "this will throw an unexpected exception"]
                                :ns       'octocat.arithmetic-test
                                :file     actual-file?
                                :expected 0
                                :error    #(instance? ArithmeticException %)
                                :type     :error}]}
                             :summary    {:error 1 :fact 4 :fail 2 :ns 1 :pass 2 :skip 0 :test 5}
                             :testing-ns 'octocat.arithmetic-test})

(def re-run-arithmetic-test-report {:results
                                    {'octocat.arithmetic-test
                                     [{:context  ["this is a crazy arithmetic"]
                                       :ns       'octocat.arithmetic-test
                                       :file     actual-file?
                                       :expected 6
                                       :actual   5
                                       :message  '()
                                       :type     :fail}
                                      {:context
                                       ["two assertions in the same fact; the former is correct while the later is wrong"]
                                       :ns   'octocat.arithmetic-test
                                       :file actual-file?
                                       :type :pass}
                                      {:context
                                       ["two assertions in the same fact; the former is correct while the later is wrong"]
                                       :ns       'octocat.arithmetic-test
                                       :file     actual-file?
                                       :expected 3
                                       :actual   2
                                       :message  '()
                                       :type     :fail}
                                      {:context  ["this will throw an unexpected exception"]
                                       :ns       'octocat.arithmetic-test
                                       :file     actual-file?
                                       :expected 0
                                       :error    #(instance? ArithmeticException %)
                                       :type     :error}]}
                                    :summary {:error 1 :fact 3 :fail 2 :ns 1 :pass 1 :skip 0 :test 4}})

(def colls-test-report {:results
                        {'octocat.colls-test
                         [{:context  ["about Clojure collections" "one key is missing in the actual map"]
                           :ns       'octocat.colls-test
                           :file     actual-file?
                           :expected {:first-name "John" :last-name "Doe"}
                           :actual   {:first-name "John"}
                           :type     :fail}
                          {:context  ["about Clojure collections" "the rightmost isn't contained into the leftmost"]
                           :ns       'octocat.colls-test
                           :file     actual-file?
                           :expected '(contains [1 2 3 4])
                           :actual   [1 2 3]
                           :type     :fail}
                          {:context  ["about Clojure collections" "the rightmost isn't contained into the leftmost"]
                           :ns       'octocat.colls-test
                           :file     actual-file?
                           :expected '(match {:elements [:b :c]})
                           :actual   {:elements [:a :b]}
                           :type     :fail}
                          {:context  ["about Clojure collections" "the leftmost doesn't have the same elements as the rightmost"]
                           :ns       'octocat.colls-test
                           :file     actual-file?
                           :expected '(match (m/in-any-order [3 2 4]))
                           :actual   [1 2 3]
                           :type     :fail}]}
                        :summary    {:error 0 :fact 3 :fail 4 :ns 1 :pass 0 :skip 0 :test 4}
                        :testing-ns 'octocat.colls-test})

(def mocks-test-report {:results
                        {'octocat.mocks-test
                         [{:context ["about prerequisits" "this one is mistakenly mocked out"]
                           :ns      'octocat.mocks-test
                           :file    actual-file?
                           :type    :fail}
                          {:context ["about prerequisits" "this one is mistakenly mocked out"]
                           :ns      'octocat.mocks-test
                           :file    actual-file?
                           :type    :fail}
                          {:context  ["about prerequisits" "this one is mistakenly mocked out"]
                           :ns       'octocat.mocks-test
                           :file     actual-file?
                           :expected {:message "Hello John!"}
                           :actual   "`an-impure-function` returned this string because it was called with an unexpected argument"
                           :message  '()
                           :type     :fail}]}
                        :summary    {:error 0 :fact 1 :fail 3 :ns 1 :pass 0 :skip 0 :test 3}
                        :testing-ns 'octocat.mocks-test})

(defn isolate-test-forms!
  "Workaround to test the re-run feature without modifying Midje counters."
  [namespace]
  (swap! test-runner/test-results update namespace
         (fn [results]
           (->> results
                (map #(update % :test-forms
                              (partial format "(with-isolated-output-counters %s)")))
                vec))))

(facts "about running individual tests"

       (fact "tests the given forms"
             (test-runner/test-forms 'midje-nrepl.test-runner-test '(with-isolated-output-counters  (fact 1 => 1))
                                     '(with-isolated-output-counters (fact (+ 3 2) => 6)))
             => (match test-forms-report))

       (fact "returns a report with no tests when there are no tests to be run"
             (test-runner/test-forms 'midje-nrepl.test-runner-test '())
             => (match {:results {}
                        :summary {:ns 0 :test 0}}))

       (fact "runs the test form passed as a string"
             (test-runner/run-test 'midje-nrepl.test-runner-test "(with-isolated-output-counters (fact 1 => 1))")
             => (match individual-test-report))

       (fact "returns a report with no tests when there are no tests to be run"
             (test-runner/run-test 'midje-nrepl.test-runner-test "(fact)")
             => (match {:results {}
                        :summary {:ns 0 :test 0}}))

       (fact "the results of the last execution are kept in the current session"
             (test-runner/run-test 'midje-nrepl.test-runner-test "(with-isolated-output-counters (fact 1 => 1))")
             => (match individual-test-report)
             @test-runner/test-results
             => (match (:results individual-test-report))))

(facts "about running tests in a given namespace"

       (tabular (fact "runs all tests in the given namespace"
                      (test-runner/run-tests-in-ns ?namespace) => (match ?report))
                ?namespace ?report
                'octocat.arithmetic-test arithmetic-test-report
                'octocat.colls-test colls-test-report
                'octocat.mocks-test mocks-test-report)

       (fact "returns a report with no tests when there are no tests to be run"
             (test-runner/run-tests-in-ns 'octocat.no-tests)
             => (match {:results {}
                        :summary {:ns 0 :test 0}}))

       (fact "the results of the last execution are kept in the current session"
             (test-runner/run-tests-in-ns 'octocat.arithmetic-test) => (match arithmetic-test-report)
             @test-runner/test-results
             => (match (:results arithmetic-test-report))))

(facts "about re-running tests"

       (fact "re-runs tests that have failed in the last execution"
             (test-runner/run-tests-in-ns 'octocat.arithmetic-test) => (match arithmetic-test-report)
             (isolate-test-forms! 'octocat.arithmetic-test)
             (test-runner/re-run-failed-tests) => (match re-run-arithmetic-test-report))

       (fact "returns a report with no tests when there are no failed tests to be run"
             (test-runner/run-test 'midje-nrepl.test-runner-test "(with-isolated-output-counters (fact (+ 1 2) => 3))")
             => (match {:summary {:error 0 :fail 0 :ns 1 :pass 1 :test 1}})
             (test-runner/re-run-failed-tests)
             => (match {:results {}
                        :summary {:ns 0 :test 0}}))

       (fact "the results of the last execution are kept in the current session as well"
             (test-runner/run-tests-in-ns 'octocat.arithmetic-test) => (match arithmetic-test-report)
             (isolate-test-forms! 'octocat.arithmetic-test)
             (test-runner/re-run-failed-tests) => (match re-run-arithmetic-test-report)
             @test-runner/test-results
             => (match (:results re-run-arithmetic-test-report))))
