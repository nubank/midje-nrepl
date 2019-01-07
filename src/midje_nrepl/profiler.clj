(ns midje-nrepl.profiler
  (:require [midje-nrepl.misc :as misc])
  (:import (java.time Duration)))

(defn duration->string
  "Returns a friendly representation of the duration object in question."
  [duration]
  (let [milliseconds (.toMillis duration)]
    (cond
      (<= milliseconds 1000)  (str milliseconds " milliseconds")
      (<= milliseconds 60000) (format "%.2f seconds" (/ milliseconds 1000.0))
      :else                   (format "%.2f minutes" (/ milliseconds
                                                        60000.0)))))

(defn- fastest-test-comparator
  "Compares two test results and determines the fastest one."
  [x y]
  (letfn [(test-duration [{:keys [started-at finished-at]}]
            (misc/duration-between started-at finished-at))]
    (.compareTo (test-duration x) (test-duration y))))

(def ^:private slowest-test-comparator
  "Compares two test results and determines the slowest one."
  (comp (partial * -1) fastest-test-comparator))

(defn- top-tests
  "Returns the top n fastest or slowest tests (depending upon the supplied comparator)."
  [comparator n test-results]
  (->> test-results
       (sort comparator)
       (take n)
       (map (fn [{:keys [started-at finished-at] :as result}]
              (assoc (select-keys result [:context :file :line])
                     :duration (misc/duration-between started-at finished-at))))))

(def top-fastest-tests
  "Returns the top n fastest tests in the test results."
  (partial top-tests fastest-test-comparator))

(def top-slowest-tests
  "Returns the top n slowest tests in the test results."
  (partial top-tests slowest-test-comparator))

(defn- duration-of-tests-in-ns [test-results]
  (let [{:keys [started-at]}  (first test-results)
        {:keys [finished-at]} (last test-results)]
    (misc/duration-between started-at finished-at)))

(defn duration-per-ns [test-results]
  (->> test-results
       (group-by :ns)
       (map #(update % 1 duration-of-tests-in-ns))
       (into {})))

(defn average
  "Returns a map describing the average of the duration of tests."
  [duration number-of-tests]
  {:duration (if (zero? number-of-tests)
               (Duration/ZERO)
               (.dividedBy duration number-of-tests))
   :tests    number-of-tests})

(defn distinct-results-with-known-durations [report-map]
  (->> report-map
       :results
       vals
       flatten
       (filter #(and (:started-at %)
                     (:finished-at %)))
       (group-by :id)
       vals
       (map last)))

(defn- assoc-stats [report-map options]
  (let [{:keys [fastest-tests slowest-tests]
         :or   {fastest-tests 1
                slowest-tests 1}} options
        test-results              (distinct-results-with-known-durations report-map)]
    (assoc report-map
           :profiling {:average         (average (get-in report-map [:summary :finished-in]) (count test-results))
                       :duration-per-ns (duration-per-ns test-results)
                       :fastest-tests   (top-fastest-tests fastest-tests test-results)
                       :slowest-tests   (top-slowest-tests slowest-tests test-results)})))

(defn profiling [runner]
  (fn [options]
    (let [start      (misc/now)
          report-map (runner options)
          end        (misc/now)]
      (-> report-map
          (assoc-in [:summary :finished-in] (misc/duration-between start end))
          (assoc-stats options)))))
