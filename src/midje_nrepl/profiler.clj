(ns midje-nrepl.profiler
  (:require [midje-nrepl.misc :as misc]))

(defn duration->string
  "Returns a friendly representation of the duration object in question."
  [duration]
  (let [milliseconds (.toMillis duration)]
    (cond
      (<= milliseconds 1000)  (str milliseconds " milliseconds")
      (<= milliseconds 60000) (format "%.2f seconds" (/ milliseconds 1000.0))
      :else                   (format "%.2f minutes" (/ milliseconds
                                                        60000.0)))))

(defn- duration-of-tests-in-ns [test-results]
  (let [{:keys [started-at]}  (first test-results)
        {:keys [finished-at]} (last test-results)]
    (misc/duration-between started-at finished-at)))

(defn- keep-distinct-tests-with-known-durations [test-results]
  (->> test-results
       (filter #(and (:started-at %)
                     (:finished-at %)))
       (group-by :id)
       vals
       (map last)))

(defn duration-per-namespace [report-map]
  (->> report-map
       :results
       (map #(update % 1 duration-of-tests-in-ns))
       (map (partial zipmap [:ns :duration]))))

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
  "Returns the top n fastest or slowest tests (depending upon the supplied comparator) in the report map."
  [comparator n report-map]
  (->> report-map
       :results
       vals
       flatten
       keep-distinct-tests-with-known-durations
       (sort comparator)
       (take n)
       (map (fn [{:keys [started-at finished-at] :as result}]
              (assoc (select-keys result [:context :file :line])
                     :duration (misc/duration-between started-at finished-at))))))

(def top-fastest-tests
  "Returns the top n fastest tests in the report map."
  (partial top-tests fastest-test-comparator))

(def top-slowest-tests
  "Returns the top n slowest tests in the report map."
  (partial top-tests slowest-test-comparator))

(defn average
  "Returns a map describing the average of the duration of tests."
  [duration number-of-tests]
  {:duration (.dividedBy duration number-of-tests)
   :tests    number-of-tests})
