(ns midje-nrepl.profiler-test
  (:require [clojure.java.io :as io]
            [matcher-combinators.matchers :as m]
            [matcher-combinators.midje :refer [match]]
            [midje-nrepl.misc :as misc]
            [midje-nrepl.profiler :as profiler]
            [midje.sweet :refer :all])
  (:import java.time.Duration))

(defn plus-millis [instant millis]
  (.plusMillis instant millis))

(defn millis->duration [millis]
  (.plusNanos (Duration/ZERO) (* millis 1000000)))

(def start-point (misc/now))

(def one-millisecond-later (plus-millis start-point 1))

(def two-milliseconds-later (plus-millis start-point 2))

(def three-milliseconds-later (plus-millis start-point 3))

(def four-milliseconds-later (plus-millis start-point 4))

(def nine-milliseconds-later (plus-millis start-point 9))

(def ten-milliseconds-later (plus-millis start-point 10))

(def thirteen-milliseconds-later (plus-millis start-point 13))

(def arithmetic-test-file (io/file "/home/john-doe/projects/octocat/test/octocat/arithmetic_test.clj"))

(def heavy-test-file (io/file "/home/john-doe/projects/octocat/test/octocat/heavy_test.clj"))

(def report-map {:results
                 {'octocat.arithmetic-test [{:context     ["First arithmetic test"]
                                             :id          "20208edc-4129-4511-be13-38c9a8e28480"
                                             :ns          'octocat.arithmetic-test
                                             :file        arithmetic-test-file
                                             :line        5
                                             :started-at  start-point
                                             :finished-at one-millisecond-later}
                                            {:context ["I am a future fact"]}
                                            {:context     ["second arithmetic test"]
                                             :id          "021a8d7f-e546-42e4-8c70-da4fcc7be6b4"
                                             :ns          'octocat.arithmetic-test
                                             :file        arithmetic-test-file
                                             :line        8
                                             :started-at  one-millisecond-later
                                             :finished-at two-milliseconds-later}
                                            {:context     ["third arithmetic test"]
                                             :id          "b64ac1c8-ff83-4785-8cec-9c180609cb9f"
                                             :ns          'octocat.arithmetic-test
                                             :file        arithmetic-test-file
                                             :line        10
                                             :started-at  two-milliseconds-later
                                             :finished-at three-milliseconds-later}
                                            {:context     ["fourth arithmetic test"]
                                             :id          "a3d7e905-8f3f-40b2-bf80-a80566a90a64"
                                             :ns          'octocat.arithmetic-test
                                             :file        arithmetic-test-file
                                             :line        13
                                             :started-at  three-milliseconds-later
                                             :finished-at four-milliseconds-later}]
                  'octocat.heavy-test      [{:context     ["First heavy test"]
                                             :id          "8a8e79c5-84aa-4846-b233-5969ec26a853"
                                             :ns          'octocat.heavy-test
                                             :file        heavy-test-file
                                             :line        5
                                             :started-at  three-milliseconds-later
                                             :finished-at nine-milliseconds-later}
                                            {:context     ["First heavy test"]
                                             :id          "8a8e79c5-84aa-4846-b233-5969ec26a853"
                                             :ns          'octocat.heavy-test
                                             :file        heavy-test-file
                                             :line        7
                                             :started-at  three-milliseconds-later
                                             :finished-at ten-milliseconds-later}
                                            {:context ["I don't have a duration"]
                                             :id      "0f9878b3-4d28-484d-9b98-a61ef53f4f89"
                                             :ns      'octocat.heavy-test}
                                            {:context ["I don't have a duration too"]
                                             :id      "2f965b08-3508-43df-969b-274a0a360009"
                                             :ns      'octocat.heavy-test}
                                            {:context     ["second heavy test"]
                                             :id          "12160c1a-4d4a-4d8d-8870-72243ee9539e"
                                             :ns          'octocat.heavy-test
                                             :file        heavy-test-file
                                             :line        12
                                             :started-at  ten-milliseconds-later
                                             :finished-at thirteen-milliseconds-later}]}
                 :summary {:check 6 :error 0 :fail 0 :ns 2 :pass 6 :to-do 3}})

(def test-results (profiler/distinct-results-with-known-durations report-map))

(def total-time (misc/duration-between start-point thirteen-milliseconds-later))

(defn fake-runner [_]
  (Thread/sleep (.toMillis total-time))
  report-map)

(def duration? #(instance? Duration %))

(facts "about the profiler"

       (tabular (fact "returns a friendly string representing the duration in question"
                      (profiler/duration->string ?duration) => ?result)
                ?duration            ?result
                (Duration/ZERO)   "0 milliseconds"
                (Duration/ofMillis 1)    "1 millisecond"
                (Duration/ofMillis 256) "256 milliseconds"
                (Duration/ofMillis 1000)         "1 second"
                (Duration/ofMillis 6537)     "6.54 seconds"
                (Duration/ofMinutes 1)         "1 minute"
                (Duration/ofMillis 63885)     "1.06 minutes"
                (Duration/ofMinutes 4)        "4 minutes")

       (fact "given the total time of the test suite and the number of tests in
       that suite, returns the average time taken by each test"
             (profiler/average (misc/duration-between start-point ten-milliseconds-later) 10)
             => (millis->duration 1))

       (fact "when the number of tests is zero, returns a zeroed duration"
             (profiler/average (Duration/ZERO) 0)
             => (Duration/ZERO))

       (fact "produces profiling statistics for each namespace"
             (profiler/stats-per-ns test-results total-time)
             => [{:ns                    'octocat.heavy-test
                  :total-time            (millis->duration 10)
                  :percent-of-total-time "76.92%"
                  :average               (millis->duration 5)
                  :number-of-tests       2}
                 {:ns                    'octocat.arithmetic-test
                  :percent-of-total-time "30.77%"
                  :total-time            (millis->duration 4)
                  :average               (millis->duration 1)
                  :number-of-tests       4}])

       (fact "returns information about the top n slowest tests in the test results"
             (profiler/top-slowest-tests 2 test-results)
             => [{:context    ["First heavy test"]
                  :file       heavy-test-file
                  :line       7
                  :total-time (millis->duration 7)}
                 {:context    ["second heavy test"]
                  :file       heavy-test-file
                  :line       12
                  :total-time (millis->duration 3)}])

       (fact "returns statistics about the time consumed by the supplied test results"
             (profiler/time-consumption (take 4 test-results) (millis->duration 13))
             => {:total-time            (millis->duration 4)
                 :percent-of-total-time "30.77%"})

       (fact "wraps a runner function by adding profiling information to the returned report data"
             ((profiler/profile fake-runner) {:profile? true})
             => (match {:profile
                        {:average         duration?
                         :total-time      duration?
                         :number-of-tests 6
                         :top-slowest-tests
                         {:tests                 (m/prefix [{:context    ["First heavy test"]
                                                             :line       7
                                                             :total-time duration?}])
                          :total-time            duration?
                          :percent-of-total-time string?}
                         :namespaces
                         [{:ns 'octocat.heavy-test}
                          {:ns 'octocat.arithmetic-test}]}
                        :summary {:finished-in duration?}}))

       (fact "when the key `:profile?` isn't set to true, the profiler only adds
       the total time taken by the test suite to the returned report map"
             (let [{:keys [profile summary]} ((profiler/profile fake-runner) {})]
               profile => nil
               (:finished-in summary) => duration?))

       (fact "the default number of slowest tests is 5"
             ((profiler/profile fake-runner) {:profile? true})
             => (match {:profile
                        {:top-slowest-tests {:tests #(= (count %) 5)}}}))

       (fact "users can customize the number of slowest tests"
             ((profiler/profile fake-runner) {:profile?      true
                                              :slowest-tests 3})
             => (match {:profile
                        {:top-slowest-tests {:tests #(= (count %) 3)}}}))

       (fact "when there are no tests, do not try to collect profile statistics"
             ((profiler/profile (constantly {:summary {:check 0}})) {:profile? true})
             => (match (m/equals {:summary {:check       0
                                            :finished-in duration?}}))))
