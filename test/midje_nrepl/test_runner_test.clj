(ns midje-nrepl.test-runner-test
  (:require [midje.sweet :refer :all]
            [matcher-combinators.midje :refer [match]]
            [midje-nrepl.test-runner :as test-runner]))

(defn actual-file? [candidate]
  (instance? java.io.File candidate))

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
                                :message  ()
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
                                :message  ()
                                :type     :fail}
                               {:context  ["about arithmetic operations" "this will throw an unexpected exception"]
                                :ns       'octocat.arithmetic-test
                                :file     actual-file?
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
                        :summary    {:error 0 :fail 4 :ns 1 :pass 0 :skip 0 :test 4}
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
                           :message  ()
                           :type     :fail}]}
                        :summary    {:error 0 :fail 3 :ns 1 :pass 0 :skip 0 :test 3}
                        :testing-ns 'octocat.mocks-test})

(tabular (fact "it runs all tests in the given namespace"
               (test-runner/run-tests-in-ns ?namespace) => (match ?report))
         ?namespace ?report
         'octocat.arithmetic-test arithmetic-test-report
         'octocat.colls-test colls-test-report
         'octocat.mocks-test mocks-test-report)
