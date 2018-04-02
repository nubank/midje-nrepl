(ns midje-nrepl.reporter-test
  (:require [matcher-combinators.matchers :as m]
            [matcher-combinators.midje :refer [match]]
            [midje-nrepl.reporter :as reporter]
            [midje.sweet :refer :all]
            [midje.util.exceptions :as midje.exceptions]))

(def correct-fact-function (with-meta (constantly true)
                             #:midje {:guid            "d2cd94c3346922886e796da80ab99ab764ba30f9"
                                      :source          '(fact "this is inquestionable" 1 => 1)
                                      :namespace       'octocat.arithmetic-test
                                      :file            "file:/home/john-doe/dev/projects/octocat/test/octocat/mocks_test.clj"
                                      :line            10
                                      :description     "this is inquestionable"
                                      :name            "this is inquestionable"
                                      :top-level-fact? true}))

(def wrong-fact-function (with-meta (constantly false)
                           #:midje {:guid            "3b649bc647f4a2e8410c2cf8f90828cc017ae13"
                                    :source          '(fact "this is wrong" 1 => 2)
                                    :namespace       'octocat.arithmetic-test
                                    :file            "file:/home/john-doe/dev/projects/octocat/test/octocat/mocks_test.clj"
                                    :line            14
                                    :description     "this is wrong"
                                    :name            "this is wrong"
                                    :top-level-fact? true}))

(def impossible-fact-function (with-meta (constantly false)
                                #:midje {:guid            "1ced1998157d66cc34a347e593ded405d59ecfe2"
                                         :source          '(fact "this is impossible" (/ 10 0) => 0)
                                         :namespace       'octocat.arithmetic-test
                                         :file            "file:/home/john-doe/dev/projects/octocat/test/octocat/mocks_test.clj"
                                         :line            20
                                         :description     "this is impossible"
                                         :name            "this is impossible"
                                         :top-level-fact? true}))

(def failure-with-simple-mismatch {:actual               1
                                   :arrow                '=>
                                   :call-form            1
                                   :check-expectation    :expect-match
                                   :description          ["this is wrong"]
                                   :expected-result      2
                                   :expected-result-form 2
                                   :function-under-test  wrong-fact-function
                                   :position             ["arithmetic_test.clj" 15]
                                   :type                 :actual-result-did-not-match-expected-value})

(def arithmetic-exception (ArithmeticException. "Divid by zero"))

(def failure-with-unexpected-exception {:actual               (midje.exceptions/captured-throwable arithmetic-exception)
                                        :arrow                '=>
                                        :call-form            '(/ 10 0)
                                        :check-expectation    :expect-match
                                        :description          ["this is impossible"]
                                        :expected-result      0
                                        :expected-result-form 0
                                        :function-under-test  impossible-fact-function
                                        :position             ["arithmetic_test.clj" 20]
                                        :type                 :actual-result-did-not-match-expected-value})

(def failure-with-checker-notes {:actual               [1 2 3]
                                 :arrow                '=>
                                 :call-form            [1 2 3]
                                 :check-expectation    :expect-match
                                 :description          ["the rightmost isn't contained into the leftmost"]
                                 :expected-result      (contains [1 2 3 4])
                                 :expected-result-form '(contains [1 2 3 4])
                                 :function-under-test  (constantly false)
                                 :notes                '("Best match found: [1 2 3]")
                                 :position             ["colls_test.clj" 12]
                                 :type                 :actual-result-did-not-match-checker})

(def failure-with-prerequisit-error {:failures '({:actual-count         0
                                                  :expected-call        "(an-impure-function {:first-name \"John\", :last-name \"Doe\"})"
                                                  :expected-count       :default
                                                  :expected-result-form "(an-impure-function {:first-name \"John\", :last-name \"Doe\"})"
                                                  :position             ["mocks_test.clj" 15]})
                                     :position ["mocks_test.clj" 15]
                                     :type     :some-prerequisites-were-called-the-wrong-number-of-times})

(facts "about the midje-nrepl's reporter"

       (fact "it resets the report atom"
             (reporter/reset-report! 'octocat.arithmetic-test)
             @reporter/report => {:testing-ns 'octocat.arithmetic-test
                                  :results    {}

                                  :summary {:error 0 :fail 0 :ns 0 :pass 0 :skip 0 :test 0}})

       (fact "when Midje starts checking a top level fact,
it stores its description in the report atom"
             (reporter/starting-to-check-top-level-fact correct-fact-function)
             @reporter/report => (match {:top-level-description ["this is inquestionable"]}))

       (fact "when Midje start checking a fact,
it stores the information about this fact in the report map"
             (reporter/starting-to-check-fact correct-fact-function)
             @reporter/report => (match {:current-test
                                         {:context    ["this is inquestionable"]
                                          :ns         'octocat.arithmetic-test
                                          :file       #(instance? java.io.File %)
                                          :line       10
                                          :test-forms "(fact \"this is inquestionable\" 1 => 1)"}}))

       (fact "when the test passes,
it stores the corresponding test result in the report atom"
             (reporter/pass)
             @reporter/report => (match {:results {'octocat.arithmetic-test
                                                   [{:context    ["this is inquestionable"]
                                                     :ns         'octocat.arithmetic-test
                                                     :file       #(instance? java.io.File %)
                                                     :line       10
                                                     :test-forms "(fact \"this is inquestionable\" 1 => 1)"
                                                     :type       :pass}]}}))

       (fact "when Midje finishes a top-level fact,
it dissoc's some keys from the report atom"
             (reporter/finishing-top-level-fact correct-fact-function)
             (keys @reporter/report) => (match (m/in-any-order [:results :summary :testing-ns])))

       (fact "when the test fails,
it stores the corresponding test result in the report atom"
             (reporter/starting-to-check-top-level-fact wrong-fact-function)
             (reporter/starting-to-check-fact wrong-fact-function)
             (reporter/fail failure-with-simple-mismatch)
             @reporter/report => (match {:results {'octocat.arithmetic-test
                                                   [{:context    ["this is inquestionable"]
                                                     :ns         'octocat.arithmetic-test
                                                     :file       #(instance? java.io.File %)
                                                     :line       10
                                                     :test-forms "(fact \"this is inquestionable\" 1 => 1)"
                                                     :type       :pass}
                                                    {:context    ["this is wrong"]
                                                     :ns         'octocat.arithmetic-test
                                                     :file       #(instance? java.io.File %)
                                                     :line       15
                                                     :test-forms "(fact \"this is wrong\" 1 => 2)"
                                                     :expected   2
                                                     :actual     1
                                                     :message    '()
                                                     :type       :fail}]}})
             (reporter/finishing-top-level-fact wrong-fact-function))

       (fact "when the test throws an unexpected exception,
it is interpreted as an error in the test report"
             (reporter/starting-to-check-top-level-fact impossible-fact-function)
             (reporter/starting-to-check-fact impossible-fact-function)
             (reporter/fail failure-with-unexpected-exception)
             @reporter/report => (match {:results {'octocat.arithmetic-test
                                                   [{:context    ["this is inquestionable"]
                                                     :ns         'octocat.arithmetic-test
                                                     :file       #(instance? java.io.File %)
                                                     :line       10
                                                     :test-forms "(fact \"this is inquestionable\" 1 => 1)"
                                                     :type       :pass}
                                                    {:context    ["this is wrong"]
                                                     :ns         'octocat.arithmetic-test
                                                     :file       #(instance? java.io.File %)
                                                     :line       15
                                                     :test-forms "(fact \"this is wrong\" 1 => 2)"
                                                     :expected   2
                                                     :actual     1
                                                     :message    '()
                                                     :type       :fail}
                                                    {:context    ["this is impossible"]
                                                     :ns         'octocat.arithmetic-test
                                                     :file       #(instance? java.io.File %)
                                                     :line       20
                                                     :test-forms "(fact \"this is impossible\" (/ 10 0) => 0)"
                                                     :expected   0
                                                     :error      #(= arithmetic-exception %)
                                                     :type       :error}]}})
             (reporter/finishing-top-level-fact impossible-fact-function))

       (fact "future facts are interpreted as skipped tests in the report"
             (reporter/future-fact ["TODO"] ["arithmetic_test.clj" 25])
             @reporter/report => (match {:results {'octocat.arithmetic-test
                                                   [{:context    ["this is inquestionable"]
                                                     :ns         'octocat.arithmetic-test
                                                     :file       #(instance? java.io.File %)
                                                     :line       10
                                                     :test-forms "(fact \"this is inquestionable\" 1 => 1)"
                                                     :type       :pass}
                                                    {:context    ["this is wrong"]
                                                     :ns         'octocat.arithmetic-test
                                                     :file       #(instance? java.io.File %)
                                                     :line       15
                                                     :test-forms "(fact \"this is wrong\" 1 => 2)"
                                                     :expected   2
                                                     :actual     1
                                                     :message    '()
                                                     :type       :fail}
                                                    {:context    ["this is impossible"]
                                                     :ns         'octocat.arithmetic-test
                                                     :file       #(instance? java.io.File %)
                                                     :line       20
                                                     :test-forms "(fact \"this is impossible\" (/ 10 0) => 0)"
                                                     :expected   0
                                                     :error      #(= arithmetic-exception %)
                                                     :type       :error}
                                                    {:context ["TODO"]
                                                     :line    25
                                                     :type    :skip}]}}))

       (fact "it summarizes test results, by computing the counters for each category"
             (reporter/summarize-test-results!)
             @reporter/report => (match {:summary {:error 1
                                                   :fail  1
                                                   :ns    1
                                                   :pass  1
                                                   :skip  1
                                                   :test  4}}))

       (tabular (fact "it explains the failures according to the failure-map's type"
                      (reporter/explain-failure ?failure-map) => ?result)
                ?failure-map                    ?result
                failure-with-simple-mismatch    {:expected 2 :actual 1 :message '()}
                failure-with-checker-notes      {:expected '(contains [1 2 3 4]) :actual [1 2 3] :message '("        Best match found: [1 2 3]")}
                failure-with-prerequisit-error  {:message '("These calls were not made the right number of times:" "    (an-impure-function {:first-name \"John\", :last-name \"Doe\"}) [expected at least once, actually never called]")})

       (fact "the macro below evaluates the forms with the reporter in context"
             (reporter/with-reporter-for 'midje-nrepl.reporter-test
               (fact "I'm pretty sure about that"
                     1 => 1))

             @reporter/report => (match {:results
                                         {'midje-nrepl.reporter-test [{:context ["I'm pretty sure about that"]
                                                                       :ns      'midje-nrepl.reporter-test
                                                                       :file    #(instance? java.io.File %)
                                                                       :line    number?}]}
                                         :summary {:error 0 :fail 0 :ns 1 :pass 1 :test 1 :skip 0}})))
