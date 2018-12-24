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

(def ten-seconds-later (plus-seconds start-point 10))

(def thirteen-seconds-later (plus-seconds start-point 13))

(def arithmetic-test-file (io/file "/home/john-doe/projects/octocat/test/octocat/arithmetic_test.clj"))

(def heavy-test-file (io/file "/home/john-doe/projects/octocat/test/octocat/heavy_test.clj"))

(def report-map {:results
                 {'octocat.arithmetic-test [{:context     ["First arithmetic test"]
                                             :file        arithmetic-test-file
                                             :line        5
                                             :started-at  start-point
                                             :finished-at one-second-later}
                                            {:context ["I am a future fact"]}
                                            {:context     ["second arithmetic test"]
                                             :file        arithmetic-test-file
                                             :line        8
                                             :started-at  one-second-later
                                             :finished-at two-seconds-later}
                                            {:context     ["third arithmetic test"]
                                             :file        arithmetic-test-file
                                             :line        10
                                             :started-at  two-seconds-later
                                             :finished-at three-seconds-later}]
                  'octocat.heavy-test      [{:context     ["First heavy test"]
                                             :file        heavy-test-file
                                             :line        5
                                             :started-at  three-seconds-later
                                             :finished-at ten-seconds-later}
                                            {:context ["I don't have a duration"]}
                                            {:context ["I don't have a duration too"]}
                                            {:context     ["second heavy test"]
                                             :file        heavy-test-file
                                             :line        8
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
                  :line     5
                  :duration (misc/duration-between three-seconds-later ten-seconds-later)}]))
