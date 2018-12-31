(ns midje-nrepl.profiler-test
  (:require [clojure.java.io :as io]
            [matcher-combinators.midje :refer [match]]
            [midje-nrepl.misc :as misc]
            [midje-nrepl.profiler :as profiler]
            [midje.sweet :refer :all])
  (:import java.time.LocalDateTime))

(defn local-date-time [seconds]
  (LocalDateTime/of 2019 01 01 12 0 seconds))

(defn plus-seconds [time seconds]
  (.plusSeconds time seconds))

(def start-point (local-date-time 0))

(def one-second-later (plus-seconds start-point 1))

(def two-seconds-later (plus-seconds start-point 2))

(def three-seconds-later (plus-seconds start-point 3))

(def nine-and-half-seconds-later (plus-seconds start-point 9.5))

(def ten-seconds-later (plus-seconds start-point 10))

(def thirteen-seconds-later (plus-seconds start-point 13))

(def arithmetic-test-file (io/file "/home/john-doe/projects/octocat/test/octocat/arithmetic_test.clj"))

(def heavy-test-file (io/file "/home/john-doe/projects/octocat/test/octocat/heavy_test.clj"))

(def report-map {:results
                 {'octocat.arithmetic-test [{:context     ["First arithmetic test"]
                                             :id          "20208edc-4129-4511-be13-38c9a8e28480"
                                             :file        arithmetic-test-file
                                             :line        5
                                             :started-at  start-point
                                             :finished-at one-second-later}
                                            {:context ["I am a future fact"]}
                                            {:context     ["second arithmetic test"]
                                             :id          "021a8d7f-e546-42e4-8c70-da4fcc7be6b4"
                                             :file        arithmetic-test-file
                                             :line        8
                                             :started-at  one-second-later
                                             :finished-at two-seconds-later}
                                            {:context     ["third arithmetic test"]
                                             :id          "b64ac1c8-ff83-4785-8cec-9c180609cb9f"
                                             :file        arithmetic-test-file
                                             :line        10
                                             :started-at  two-seconds-later
                                             :finished-at three-seconds-later}]
                  'octocat.heavy-test      [{:context     ["First heavy test"]
                                             :id          "8a8e79c5-84aa-4846-b233-5969ec26a853"
                                             :file        heavy-test-file
                                             :line        5
                                             :started-at  three-seconds-later
                                             :finished-at nine-and-half-seconds-later}
                                            {:context     ["First heavy test"]
                                             :id          "8a8e79c5-84aa-4846-b233-5969ec26a853"
                                             :file        heavy-test-file
                                             :line        7
                                             :started-at  three-seconds-later
                                             :finished-at ten-seconds-later}
                                            {:context ["I don't have a duration"]
                                             :id      "0f9878b3-4d28-484d-9b98-a61ef53f4f89"}
                                            {:context ["I don't have a duration too"]
                                             :id      "2f965b08-3508-43df-969b-274a0a360009"}
                                            {:context     ["second heavy test"]
                                             :id          "12160c1a-4d4a-4d8d-8870-72243ee9539e"
                                             :file        heavy-test-file
                                             :line        12
                                             :started-at  ten-seconds-later
                                             :finished-at thirteen-seconds-later}]}})

(facts "about the profiler"

       (fact "computes the duration of tests for each namespace"
             (profiler/duration-per-namespace report-map)
             => [{:ns       'octocat.arithmetic-test
                  :duration (misc/duration-between start-point three-seconds-later)}
                 {:ns       'octocat.heavy-test
                  :duration (misc/duration-between three-seconds-later thirteen-seconds-later)}])

       (fact "returns information about the slowest test in the report map"
             (profiler/top-slowest-tests 1 report-map)
             => [{:context  ["First heavy test"]
                  :file     heavy-test-file
                  :line     7
                  :duration (misc/duration-between three-seconds-later ten-seconds-later)}])

       (fact "returns information about the top n slowest tests in the report map"
             (profiler/top-slowest-tests 2 report-map)
             => [{:context  ["First heavy test"]
                  :file     heavy-test-file
                  :line     7
                  :duration (misc/duration-between three-seconds-later ten-seconds-later)}
                 {:context  ["second heavy test"]
                  :file     heavy-test-file
                  :line     12
                  :duration (misc/duration-between ten-seconds-later thirteen-seconds-later)}]))
