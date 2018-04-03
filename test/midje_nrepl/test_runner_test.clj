(ns midje-nrepl.test-runner-test
  (:require [clojure.test :refer :all]
            [matcher-combinators.matchers]
            [matcher-combinators.test]
            [midje-nrepl.test-runner :as test-runner]))

(defn actual-file? [candidate]
  (instance? java.io.File candidate))

(def arithmetic-test-report {:results
                             {'octocat.arithmetic-test
                              [{:context ["about arithmetic operations" "(fact (* 2 5) => 10 :position (pointer.core/line-number-known 6))"]
                                :ns      'octocat.arithmetic-test
                                :file    actual-file?
                                :line    6
                                :type    :pass}
                               {:context  ["about arithmetic operations" "this is a crazy arithmetic"]
                                :ns       'octocat.arithmetic-test
                                :file     actual-file?
                                :line     9
                                :expected 6
                                :actual   5
                                :message  ()
                                :type     :fail}
                               {:context
                                ["about arithmetic operations" "two assertions in the same fact; the former is correct while the later is wrong"]
                                :ns   'octocat.arithmetic-test
                                :file actual-file?
                                :line 11
                                :type :pass}
                               {:context
                                ["about arithmetic operations" "two assertions in the same fact; the former is correct while the later is wrong"]
                                :ns       'octocat.arithmetic-test
                                :file     actual-file?
                                :line     13
                                :expected 3
                                :actual   2
                                :message  ()
                                :type     :fail}
                               {:context  ["about arithmetic operations" "this will throw an unexpected exception"]
                                :ns       'octocat.arithmetic-test
                                :file     actual-file?
                                :line     16
                                :expected 0
                                :error    #(instance? ArithmeticException %)
                                :type     :error}]}
                             :summary    {:error 1 :fail 2 :ns 1 :pass 2 :skip 0 :test 5}
                             :testing-ns 'octocat.arithmetic-test})

(def colls-test-report {:results
                        {'octocat.colls-test
                         [{:context  ["about Clojure collections" "one key is missing in the actual map"]
                           :ns       'octocat.colls-test
                           :file     actual-file?
                           :line     7
                           :expected {:first-name "John" :last-name "Doe"}
                           :actual   {:first-name "John"}
                           :type     :fail}
                          {:context  ["about Clojure collections" "the rightmost isn't contained into the leftmost"]
                           :ns       'octocat.colls-test
                           :file     actual-file?
                           :line     12
                           :expected '(contains [1 2 3 4])
                           :actual   [1 2 3]
                           :type     :fail}
                          {:context  ["about Clojure collections" "the rightmost isn't contained into the leftmost"]
                           :ns       'octocat.colls-test
                           :file     actual-file?
                           :line     14
                           :expected '(match {:elements [:b :c]})
                           :actual   {:elements [:a :b]}
                           :type     :fail}
                          {:context  ["about Clojure collections" "the leftmost doesn't have the same elements as the rightmost"]
                           :ns       'octocat.colls-test
                           :file     actual-file?
                           :line     17
                           :expected '(match (m/in-any-order [3 2 4]))
                           :actual   [1 2 3]
                           :type     :fail}]}
                        :summary    {:error 0 :fail 4 :ns 1 :pass 0 :skip 0 :test 4}
                        :testing-ns 'octocat.colls-test})

(def mocks-test-report {:results
                        {'octocat.mocks-test
                         [{:context ["about prerequisits" "this one is mistakenly mocked out"]
                           :ns      'octocat.mocks-test
                           :file    actual-file?
                           :line    15
                           :type    :fail}
                          {:context ["about prerequisits" "this one is mistakenly mocked out"]
                           :ns      'octocat.mocks-test
                           :file    actual-file?
                           :line    15
                           :type    :fail}
                          {:context  ["about prerequisits" "this one is mistakenly mocked out"]
                           :ns       'octocat.mocks-test
                           :file     actual-file?
                           :line     13
                           :expected {:message "Hello John!"}
                           :actual   "`an-impure-function` returned this string because it was called with an unexpected argument"
                           :message  ()
                           :type     :fail}
                          {:context ["about prerequisits" "one day this will be real"]
                           :ns      'octocat.mocks-test
                           :file    actual-file?
                           :line    18
                           :type    :skip}]}
                        :summary    {:error 0 :fail 3 :ns 1 :pass 0 :skip 1 :test 4}
                        :testing-ns 'octocat.mocks-test})

(deftest run-tests-in-ns-test
  (testing "it runs all tests in the given namespace"
    (are [namespace report] (match? report (test-runner/run-tests-in-ns namespace))
      'octocat.arithmetic-test arithmetic-test-report
      'octocat.colls-test      colls-test-report
      'octocat.mocks-test      mocks-test-report)))
