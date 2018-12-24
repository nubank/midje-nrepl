(ns midje-nrepl.profiler
  (:require [midje-nrepl.misc :as misc]))

(defn- duration-of-tests-in-ns [test-results]
  (let [{:keys [started-at]}  (first test-results)
        {:keys [finished-at]} (last test-results)]
    (misc/duration-between started-at finished-at)))

(defn- keep-only-tests-with-a-known-duration [test-results]
  (filter #(and (:started-at %)
                (:finished-at %)) test-results))

(defn duration-per-namespace [report-map]
  (->> report-map
       :results
       (map #(update % 1 duration-of-tests-in-ns))
       (map (partial zipmap [:ns :duration]))))

(defn- slowest-test-comparator
  "Compare two test results and determine the slowest of them."
  [x y]
  (letfn [(test-duration [{:keys [started-at finished-at]}]
            (misc/duration-between started-at finished-at))]
    (* -1
       (.compareTo (test-duration x) (test-duration y)))))

(defn top-slowest-tests
  "Return the top n slowest tests in the report map."
  [n report-map]
  (->> report-map
       :results
       vals
       flatten
       keep-only-tests-with-a-known-duration
       (sort slowest-test-comparator)
       (take n)
       (map (fn [{:keys [started-at finished-at] :as result}]
              (assoc (select-keys result [:context :file :line])
                     :duration (misc/duration-between started-at finished-at))))))
