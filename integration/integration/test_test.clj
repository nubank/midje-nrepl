(ns integration.test-test
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [integration.helpers :refer [send-message]]
            [matcher-combinators.matchers :as m]
            [matcher-combinators.midje :refer [match]]
            [midje.sweet :refer :all]))

(defn existing-file? [candidate]
  (-> candidate io/file .exists))

(defn read-line-from [file-path line]
  (-> (io/file file-path)
      slurp
      (string/split (re-pattern (System/lineSeparator)))
      (nth (dec line))))

(facts "about running tests"

       (fact "the REPL is up and running"
             (send-message {:op "eval" :code "(+ 2 1)"})
             => (match (list {:value "3"} {:status ["done"]})))

       (fact "runs all tests in the specified namespace"
             (send-message {:op "midje-test-ns" :ns "octocat.arithmetic-test"})
             => (match (list {:results (complement empty?)
                              :summary {:check 5 :error 1 :fact 4 :fail 2 :finished-in string? :ns 1 :pass 2 :to-do 0}}
                             {:status ["done"]})))

       (fact "when the ns is missing in the message, the middleware returns an error"
             (first (send-message {:op "midje-test-ns"}))
             => (match {:status (m/in-any-order ["done" "error" "no-ns"])}))

       (fact "the test results contain a valid path to the file at which the tests are declared"
             (->> (send-message {:op "midje-test-ns" :ns "octocat.arithmetic-test"})
                  first
                  :results
                  :octocat.arithmetic-test
                  (map :file))
             => #(every? existing-file? %))

       (fact "by following the file and the line returned by a given test result,
it's possible to jump to the correct position of the test in question"
             (let [{:keys [file line]} (->> (send-message {:op "midje-test-ns" :ns "octocat.arithmetic-test"})
                                            first
                                            :results
                                            :octocat.arithmetic-test
                                            second)]
               (read-line-from file line)
               => #"\(\+ 2 3\) => 6"))

       (fact "runs the specified test"
             (send-message {:op     "midje-test"
                            :ns     "octocat.arithmetic-test"
                            :source "(fact \"this is a crazy arithmetic\"
             (+ 2 3) => 6)"})
             => (match (list {:results
                              {:octocat.arithmetic-test
                               [{:context  ["this is a crazy arithmetic"]
                                 :index    0
                                 :ns       "octocat.arithmetic-test"
                                 :type     "fail"
                                 :expected "6\n"
                                 :actual   "5\n"
                                 :message  []}]}
                              :summary {:check 1 :error 0 :fact 1 :fail 1 :finished-in string? :ns 1 :pass 0 :to-do 0}}
                             {:status ["done"]})))

       (fact "when a line is provided, it will be used to determine the correct position of the failure in question"
             (send-message {:op     "midje-test"
                            :ns     "octocat.arithmetic-test"
                            :source "(fact \"this is a crazy arithmetic\"
             (+ 2 3) => 6)"
                            :line   9})
             => (match (list {:results
                              {:octocat.arithmetic-test
                               [{:type "fail"
                                 :line 10}]}
                              :summary {:check 1 :error 0 :fact 1 :fail 1 :ns 1 :pass 0 :to-do 0}}
                             {:status ["done"]})))

       (fact "returns proper descriptions for failing tabular facts"
             (send-message {:op     "midje-test"
                            :ns     "octocat.arithmetic-test"
                            :source "(tabular (fact \"some crazy additions\"
               (+ ?x ?y) => ?result)
  ?x ?y ?result
   5  6      12)"})
             => (match [{:results
                         {:octocat.arithmetic-test [{:context ["some crazy additions"
                                                               "With table substitutions:"
                                                               "?x 5"
                                                               "?y 6"
                                                               "?result 12"]}]}}
                        {:status (m/in-any-order ["done"])}]))

       (fact "returns the top level description for a failing tabular fact when it is inside a `facts` form"
             (send-message {:op     "midje-test"
                            :ns     "octocat.arithmetic-test"
                            :source "(facts \"about crazy operations\"
(tabular (fact \"some crazy additions\"
               (+ ?x ?y) => ?result)
  ?x ?y ?result
   5  6      12))"})
             => (match [{:results
                         {:octocat.arithmetic-test [{:context ["about crazy operations"
                                                               "some crazy additions"
                                                               "With table substitutions:"
                                                               "?x 5"
                                                               "?y 6"
                                                               "?result 12"]}]}}
                        {:status (m/in-any-order ["done"])}]))

       (fact "when the parameters ns and/or source are missing in the message,
the middleware returns an error"
             (first (send-message {:op "midje-test"}))
             => (match {:status (m/in-any-order ["done" "error" "no-ns" "no-source"])}))

       (fact "returns an error result when the test namespace is broken"
             (send-message {:op     "midje-test"
                            :ns     "octocat.arithmetic-test"
                            :source "(ns octocat.arithmetic-test
(:require [midje.sweet :refer :all]))

(boom!)"})
             => (match (list {:results
                              {:octocat.arithmetic-test [{:error #"Unable to resolve symbol: boom! in this context"
                                                          :line  4
                                                          :type  "error"}]}
                              :summary {:error 1 :finished-in string? :ns 1}}
                             {:status ["done"]})))

       (fact "runs all tests in the project"
             (send-message {:op "midje-test-all"})
             => (match (list {:results {:octocat.arithmetic-test       (complement empty?)
                                        :octocat.side-effects-test     (complement empty?)
                                        :integration.microservice-test (complement empty?)}
                              :summary {:check 8 :error 1 :fact 7 :fail 3 :finished-in string? :ns 3 :pass 4 :to-do 0}}
                             {:status ["done"]})))

       (fact "runs all tests in the specified test path"
             (send-message {:op         "midje-test-all"
                            :test-paths ["test"]})
             => (match (list {:results {:octocat.arithmetic-test   (complement empty?)
                                        :octocat.side-effects-test (complement empty?)}
                              :summary {:check 6 :error 1 :fact 5 :fail 2 :finished-in string? :ns 2 :pass 3 :to-do 0}}
                             {:status ["done"]})))

       (fact "uses ns-exclusions/ns-inclusions to test only a subset of namespaces"
             (-> (send-message {:op            "midje-test-all"
                                :ns-inclusions ["^octocat"]
                                :ns-exclusions ["side-effects-test"]})
                 first
                 :results
                 keys)
             => (match [:octocat.arithmetic-test]))

       (fact "uses test-exclusions/test-inclusions to test only a subset of tests"
             (-> (send-message {:op              "midje-test-all"
                                :test-inclusions ["mark1"]})
                 first
                 :results
                 keys)
             => (match [:octocat.side-effects-test])

             (-> (send-message {:op              "midje-test-all"
                                :test-exclusions ["mark1"]})
                 first
                 :results
                 keys)
             => (match (m/in-any-order [:octocat.arithmetic-test :integration.microservice-test])))

       (fact "clients can collect profiling information by sending the parameter
       `profile?` in the request"
             (-> (send-message {:op       "midje-test-all"
                                :profile? "true"})
                 first
                 :profile)
             => (match {:average         string?
                        :namespaces
                        (m/in-any-order [{:average               string?
                                          :ns                    "octocat.arithmetic-test"
                                          :number-of-tests       4
                                          :percent-of-total-time string?
                                          :total-time            string?}
                                         {:average               string?
                                          :ns                    "integration.microservice-test"
                                          :number-of-tests       2
                                          :percent-of-total-time string?
                                          :total-time            string?}
                                         {:average               string?
                                          :ns                    "octocat.side-effects-test"
                                          :number-of-tests       1
                                          :percent-of-total-time string?
                                          :total-time            string?}])
                        :number-of-tests 7
                        :top-slowest-tests
                        (complement empty?)
                        :total-time      string?}))

       (fact "re-runs tests that didn't pass in the previous execution"
             (send-message {:op "midje-test-ns" :ns "octocat.arithmetic-test"})
             => irrelevant
             (send-message {:op "midje-retest"})
             => (match (list {:results (complement empty?)
                              :summary {:check 4 :error 1 :fact 3 :fail 2 :finished-in string? :ns 1 :pass 1 :to-do 0}}
                             {:status ["done"]})))

       (fact "gets the stacktrace of the given erring test"
             (let [namespace          "octocat.arithmetic-test"
                   {:keys [ns index]} (->> (send-message {:op "midje-test-ns" :ns namespace})
                                           first
                                           (#(get-in % [:results (keyword namespace)]))
                                           (filter #(= "error" (:type %)))
                                           first)]
               (send-message {:op       "midje-test-stacktrace"
                              :ns       ns
                              :index    index
                              :print-fn "clojure.core/println"})
               => (match [{:class      "java.lang.ArithmeticException"
                           :message    "Divide by zero"
                           :stacktrace (complement empty?)}
                          {:status ["done"]}])))

       (fact "returns a `no-stacktrace` status when there is no stacktrace for the specified test"
             (send-message {:op       "midje-test-stacktrace"
                            :ns       "octocat.arithmetic-test"
                            :index    0
                            :print-fn "clojure.core/println"})
             => (match [{:status ["no-stacktrace"]}
                        {:status ["done"]}]))

       (fact "when the parameters ns, index and/or print-fn are missing in the message,
the middleware returns an error"
             (first (send-message {:op "midje-test-stacktrace"}))
             => (match {:status (m/in-any-order ["done" "error" "no-ns" "no-index" "no-print-fn"])})))
