(ns midje-nrepl.runner-test
  (:require [matcher-combinators.matchers :as m]
            [matcher-combinators.midje :refer [match]]
            [midje-nrepl.project-info :as project-info]
            [midje-nrepl.runner :as runner]
            [midje.sweet :refer :all]))

(defn existing-file? [candidate]
  (and   (instance? java.io.File candidate)
         (.exists candidate)))

(def individual-test-report {:results
                             {'octocat.arithmetic-test
                              [{:ns      'octocat.arithmetic-test
                                :file    existing-file?
                                :context ["(fact 1 => 1)"]
                                :index   0
                                :source  "(fact 1 => 1)"
                                :type    :pass}]}
                             :summary {:check 1 :ns 1 :fact 1 :fail 0 :error 0 :pass 1 :to-do 0}})

(def arithmetic-test-report {:results
                             {'octocat.arithmetic-test
                              [{:context ["about arithmetic operations" "(fact (* 2 5) => 10 :position (pointer.core/line-number-known 9))"]
                                :index   0
                                :ns      'octocat.arithmetic-test
                                :file    existing-file?
                                :type    :pass}
                               {:context  ["about arithmetic operations" "this is a crazy arithmetic"]
                                :index    1
                                :ns       'octocat.arithmetic-test
                                :file     existing-file?
                                :expected "6\n"
                                :actual   "5\n"
                                :message  '()
                                :type     :fail}
                               {:context
                                ["about arithmetic operations" "two assertions in the same fact; the former is correct while the later is wrong"]
                                :index 2
                                :ns    'octocat.arithmetic-test
                                :file  existing-file?
                                :type  :pass}
                               {:context
                                ["about arithmetic operations" "two assertions in the same fact; the former is correct while the later is wrong"]
                                :index    3
                                :ns       'octocat.arithmetic-test
                                :file     existing-file?
                                :expected "3\n"
                                :actual   "2\n"
                                :message  '()
                                :type     :fail}
                               {:context  ["about arithmetic operations" "this will throw an unexpected exception"]
                                :index    4
                                :ns       'octocat.arithmetic-test
                                :file     existing-file?
                                :expected "0\n"
                                :error    #(instance? ArithmeticException %)
                                :type     :error}]}
                             :summary {:check 5 :error 1 :fact 4 :fail 2 :ns 1 :pass 2 :to-do 0}})

(def re-run-arithmetic-test-report {:results
                                    {'octocat.arithmetic-test
                                     [{:context  ["this is a crazy arithmetic"]
                                       :index    0
                                       :ns       'octocat.arithmetic-test
                                       :file     existing-file?
                                       :expected "6\n"
                                       :actual   "5\n"
                                       :message  '()
                                       :type     :fail}
                                      {:context
                                       ["two assertions in the same fact; the former is correct while the later is wrong"]
                                       :index 1
                                       :ns    'octocat.arithmetic-test
                                       :file  existing-file?
                                       :type  :pass}
                                      {:context
                                       ["two assertions in the same fact; the former is correct while the later is wrong"]
                                       :index    2
                                       :ns       'octocat.arithmetic-test
                                       :file     existing-file?
                                       :expected "3\n"
                                       :actual   "2\n"
                                       :message  '()
                                       :type     :fail}
                                      {:context  ["this will throw an unexpected exception"]
                                       :index    3
                                       :ns       'octocat.arithmetic-test
                                       :file     existing-file?
                                       :expected "0\n"
                                       :error    #(instance? ArithmeticException %)
                                       :type     :error}]}
                                    :summary {:check 4 :error 1 :fact 3 :fail 2 :ns 1 :pass 1 :to-do 0}})

(def colls-test-report {:results
                        {'octocat.colls-test
                         [{:context  ["about Clojure collections" "one key is missing in the actual map"]
                           :index    0
                           :ns       'octocat.colls-test
                           :file     existing-file?
                           :expected "{:first-name \"John\", :last-name \"Doe\"}\n"
                           :actual   "{:first-name \"John\"}\n"
                           :type     :fail}
                          {:context  ["about Clojure collections" "the rightmost isn't contained into the leftmost"]
                           :index    1
                           :ns       'octocat.colls-test
                           :file     existing-file?
                           :expected "(contains [1 2 3 4])\n"
                           :actual   "[1 2 3]\n"
                           :type     :fail}
                          {:context  ["about Clojure collections" "the rightmost isn't contained into the leftmost"]
                           :index    2
                           :ns       'octocat.colls-test
                           :file     existing-file?
                           :expected "(match {:elements [:b :c]})\n"
                           :actual   "{:elements [:a :b]}\n"
                           :type     :fail}
                          {:context  ["about Clojure collections" "the leftmost doesn't have the same elements as the rightmost"]
                           :index    3
                           :ns       'octocat.colls-test
                           :file     existing-file?
                           :expected "(match (m/in-any-order [3 2 4]))\n"
                           :actual   "[1 2 3]\n"
                           :type     :fail}]}
                        :summary {:check 4 :error 0 :fact 3 :fail 4 :ns 1 :pass 0 :to-do 0}})

(def mocks-test-report {:results
                        {'octocat.mocks-test
                         [{:context ["about prerequisits" "this one is mistakenly mocked out"]
                           :index   0
                           :ns      'octocat.mocks-test
                           :file    existing-file?
                           :type    :fail}
                          {:context ["about prerequisits" "this one is mistakenly mocked out"]
                           :index   1
                           :ns      'octocat.mocks-test
                           :file    existing-file?
                           :type    :fail}
                          {:context  ["about prerequisits" "this one is mistakenly mocked out"]
                           :index    2
                           :ns       'octocat.mocks-test
                           :file     existing-file?
                           :expected "{:message \"Hello John!\"}\n"
                           :actual   "\"`an-impure-function` returned this string because it was called with an unexpected argument\"\n"
                           :message  '()
                           :type     :fail}]}
                        :summary {:check 3 :error 0 :fact 1 :fail 3 :ns 1 :pass 0 :to-do 0}})

(def all-tests-report {:results (merge (:results arithmetic-test-report)
                                       (:results colls-test-report)
                                       (:results mocks-test-report))
                       :summary {:check 12 :error 1 :fact 8 :fail 9 :ns 3 :pass 2 :to-do 0}})

(defn isolate-test-forms!
  "Workaround to test the re-run feature without modifying Midje counters."
  [namespace]
  (swap! runner/test-results update namespace
         (fn [results]
           (->> results
                (map #(update % :source
                              (partial format "(with-isolated-output-counters %s)")))
                vec))))

(facts "about running tests in a given namespace"

       (fact "runs all tests in the given namespace"
             (runner/run-tests-in-ns {:ns 'octocat.arithmetic-test}) => (match arithmetic-test-report)
             (runner/run-tests-in-ns {:ns 'octocat.colls-test}) => (match colls-test-report)
             (runner/run-tests-in-ns {:ns 'octocat.mocks-test}) => (match mocks-test-report))

       (fact "returns a report with no tests when there are no tests to be run"
             (runner/run-tests-in-ns {:ns 'octocat.no-tests})
             => (match {:results {}
                        :summary {:check 0 :ns 0}}))

       (fact "results of the last execution are kept in the current session"
             (runner/run-tests-in-ns {:ns 'octocat.arithmetic-test}) => (match arithmetic-test-report)
             @runner/test-results
             => (match (:results arithmetic-test-report))))

(facts "about running individual tests"

       (fact "runs the test source passed as a string"
             (runner/run-test {:ns 'octocat.arithmetic-test :source "(with-isolated-output-counters (fact 1 => 1))"})
             => (match individual-test-report))

       (fact "line numbers are resolved correctly for individual facts, taking the supplied starting line in consideration"
             (runner/run-test {:ns 'octocat.arithmetic-test :source "(with-isolated-output-counters
(fact \"this is wrong\"
1 => 2))"                          :line                    10})
             => (match {:results {'octocat.arithmetic-test
                                  [{:type :fail
                                    :line 12}]}}))

       (fact "returns a report with no tests when there are no tests to be run"
             (runner/run-test {:ns 'octocat.arithmetic-test :source "(fact)"})
             => (match {:results {}
                        :summary {:check 0 :ns 0}}))

       (fact "results of the last execution are kept in the current session"
             (runner/run-test {:ns 'octocat.arithmetic-test :source "(with-isolated-output-counters (fact 1 => 1))"})
             => (match individual-test-report)
             @runner/test-results
             => (match (:results individual-test-report))))

(defn checked-fact-groups [report]
  (->> report
       :results
       vals
       (map first)
       (map (comp first :context))
       distinct))

(facts "about running all tests in the project"

       (fact "runs all tests in the project according to supplied options"
             (runner/run-all-tests {:test-paths ["test/octocat"]})
             => (match all-tests-report))

       (fact "when `test-paths` isn't set, uses the test paths declared in the project"
             (runner/run-all-tests {})
             => (match all-tests-report)
             (provided
              (project-info/get-test-paths) => ["test/octocat"]))

       (tabular (fact "`:ns-exclusions` and `:ns-inclusions` allow for users to
              filter out the list of namespaces to be tested"
                      (-> (runner/run-all-tests {:test-paths    ["test/octocat"]
                                                 :ns-exclusions ?exclusions
                                                 :ns-inclusions ?inclusions})
                          :results
                          keys)
                      => ?results)
                ?exclusions             ?inclusions                                                                                    ?results
                [#"mocks"]                     nil                     (match (m/in-any-order ['octocat.arithmetic-test 'octocat.colls-test]))
                nil         [#"arithmetic"]                                                                  ['octocat.arithmetic-test]
                [#"^octocat"]                     nil                                                                                         nil
                nil           [#"^octocat"] (match (m/in-any-order ['octocat.arithmetic-test 'octocat.colls-test 'octocat.mocks-test]))
                [#"coll" #"mock"]                     nil                                                                  ['octocat.arithmetic-test]
                nil [#"^foo" #"arithmetic"]                                                                  ['octocat.arithmetic-test])

       (fact "when both `:ns-exclusions` and `:ns-inclusions` are present, the
       former takes precedence over the later"
             (runner/run-all-tests {:test-paths    ["test/octocat"]
                                    :ns-exclusions [#"arithmetic"]
                                    :ns-inclusions [#"arithmetic"]})
             => (match {:results empty?}))

       (tabular (fact "`:test-exclusions` and `:test-inclusions` allow for users
                             to filter out the list of facts to be checked"
                      (->> (runner/run-all-tests {:test-paths      ["test/octocat"]
                                                  :test-exclusions ?exclusions
                                                  :test-inclusions ?inclusions})
                           checked-fact-groups) => (match (m/in-any-order ?result)))
                ?exclusions     ?inclusions                                            ?result
                [:mark1]             nil ["about Clojure collections" "about prerequisits"]
                [:mark1 :mark2]             nil                             ["about prerequisits"]
                nil        [:mark1]                    ["about arithmetic operations"]
                nil [:mark2 :mark3] ["about Clojure collections" "about prerequisits"])

       (fact "when both `:test-exclusions` and `:test-inclusions` are present, the former takes precedence over the later"
             (->> (runner/run-all-tests {:test-paths      ["test/octocat"]
                                         :test-exclusions [:mark1]
                                         :test-inclusions [:mark1 :mark3]})
                  checked-fact-groups)
             => ["about prerequisits"])

       (fact "returns a report with no tests when there are no tests to be run"
             (runner/run-all-tests {:test-paths ["test/octocat"]})
             => (match {:results {}
                        :summary {:check 0 :ns 0}})
             (provided
              (project-info/find-namespaces-in ["test/octocat"]) => ['octocat.no-tests]))

       (fact "results of the last execution are kept in the current session"
             (runner/run-all-tests {:test-paths ["test/octocat"]})
             => (match all-tests-report)
             @runner/test-results
             => (match (:results all-tests-report))))

(facts "about re-running tests"

       (fact "re-runs tests that didn't pass in the last execution"
             (runner/run-tests-in-ns {:ns 'octocat.arithmetic-test}) => (match arithmetic-test-report)
             (isolate-test-forms! 'octocat.arithmetic-test)
             (runner/re-run-non-passing-tests nil) => (match re-run-arithmetic-test-report))

       (fact "returns a report with no tests when there are no failing or erring tests to be run"
             (runner/run-test {:ns 'octocat.arithmetic-test :source "(with-isolated-output-counters (fact (+ 1 2) => 3))"})
             => (match {:summary {:error 0 :fail 0 :ns 1 :pass 1 :check 1}})
             (runner/re-run-non-passing-tests nil)
             => (match {:results {}
                        :summary {:check 0 :ns 0}}))

       (fact "results of the last execution are kept in the current session as well"
             (runner/run-tests-in-ns {:ns 'octocat.arithmetic-test}) => (match arithmetic-test-report)
             (isolate-test-forms! 'octocat.arithmetic-test)
             (runner/re-run-non-passing-tests nil) => (match re-run-arithmetic-test-report)
             @runner/test-results
             => (match (:results re-run-arithmetic-test-report)))

       (fact "by re-running the same tests twice, the same results are obtained"
             (runner/run-tests-in-ns {:ns 'octocat.arithmetic-test}) => (match arithmetic-test-report)
             (isolate-test-forms! 'octocat.arithmetic-test)
             (runner/re-run-non-passing-tests nil) => (match re-run-arithmetic-test-report)
             (isolate-test-forms! 'octocat.arithmetic-test)
             (runner/re-run-non-passing-tests nil) => (match re-run-arithmetic-test-report)))

(facts "about getting test stacktraces"

       (fact "given a namespace symbol and an index, returns the exception stored at these coordinates"
             (runner/run-tests-in-ns {:ns 'octocat.arithmetic-test}) => irrelevant
             (runner/get-exception-at 'octocat.arithmetic-test 4) => #(instance? ArithmeticException %))

       (tabular (fact "when there is no exception at the supplied position, returns nil"
                      (runner/get-exception-at ?ns ?index) => nil)
                ?ns                       ?index
                'octocat.arithmetic-test       0
                'octocat.arithmetic-test      10
                'octocat.colls-test            2))
