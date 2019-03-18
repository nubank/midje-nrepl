(ns midje-nrepl.profiler
  (:require [midje-nrepl.misc :as misc])
  (:import java.time.Duration))

(defn- format-duration [value time-unit]
  (str (misc/format-decimal value) " "
       (if (= (float value) 1.0)
         (name time-unit)
         (str (name time-unit) "s"))))

(defn duration->string
  "Returns a friendly representation of the duration object in question."
  [duration]
  (let [millis (.toMillis duration)]
    (cond
      (<= millis 999)   (format-duration millis :millisecond)
      (<= millis 59999) (format-duration (float (/ millis 1000)) :second)
      :else             (format-duration (float (/ millis 60000)) :minute))))

(defn- slowest-test-comparator
  "Compares two test results and determines the slowest one."
  [x y]
  (* -1 (.compareTo (:total-time x) (:total-time y))))

(defn top-slowest-tests
  "Returns the top n slowest tests in the test results."
  [n test-results]
  (->> test-results
       (sort slowest-test-comparator)
       (take n)
       (map #(select-keys % [:context :total-time :file :line]))))

(defn time-consumption
  "Returns statistics about the time taken by the supplied test results."
  [test-results total-time]
  (let [total-time-of-group   (reduce (fn [total {:keys [total-time]}]
                                        (.plus total total-time)) (Duration/ZERO) test-results)
        percent-of-total-time (float (/ (.. total-time-of-group (multipliedBy 100) toMillis)
                                        (.toMillis total-time)))]
    {:total-time            total-time-of-group
     :percent-of-total-time (misc/percent percent-of-total-time)}))

(defn average
  "Returns the average time taken by each test in the test suite."
  [total-time number-of-tests]
  (if (zero? number-of-tests)
    (Duration/ZERO)
    (.dividedBy total-time number-of-tests)))

(defn- stats-for-ns [ns test-results total-time-of-suite]
  (let [number-of-tests (count test-results)]
    (let [{:keys [total-time] :as time-consumption-data} (time-consumption test-results total-time-of-suite)]
      (into {:ns              ns
             :number-of-tests number-of-tests
             :average         (average total-time (count test-results))}
            time-consumption-data))))

(defn stats-per-ns
  "Returns statistics about each tested namespace."
  [test-results total-time]
  (->> test-results
       (group-by :ns)
       (map (fn [[ns test-results]]
              (stats-for-ns ns test-results total-time)))
       (sort slowest-test-comparator)))

(defn distinct-results-with-known-durations [report-map]
  (->> report-map
       :results
       vals
       flatten
       (filter #(and (:started-at %)
                     (:finished-at %)))
       (group-by :id)
       vals
       (map last)
       (map (fn [{:keys [started-at finished-at] :as test-data}]
              (assoc test-data :total-time (misc/duration-between started-at finished-at))))))

(defn- assoc-stats
  "Assoc's profiling statistics to the report map and returns it."
  [report-map options]
  (let [{:keys [slowest-tests]
         :or   {slowest-tests 5}} options
        test-results              (distinct-results-with-known-durations report-map)
        total-time                (get-in report-map [:summary :finished-in])
        number-of-tests           (count test-results)
        top-slowest-tests         (top-slowest-tests slowest-tests test-results)]
    (assoc report-map
           :profile {:average           (average total-time number-of-tests)
                     :total-time        total-time
                     :number-of-tests   number-of-tests
                     :top-slowest-tests (into {:tests top-slowest-tests}
                                              (time-consumption top-slowest-tests total-time))
                     :namespaces        (stats-per-ns test-results total-time)})))

(defn profile
  "Wraps a runner function by including profiling statistics to the produced report map."
  [runner]
  (fn [{:keys [profile?] :as options}]
    (let [start      (misc/now)
          report-map (runner options)
          end        (misc/now)]
      (-> report-map
          (assoc-in [:summary :finished-in] (misc/duration-between start end))
          (cond-> (and profile?
                       (not (zero? (get-in report-map [:summary :check]))))
            (assoc-stats options))))))
