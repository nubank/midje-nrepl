(ns midje-nrepl.profiler
  (:require [midje-nrepl.misc :as misc]))

(defn- duration-of-tests-in-ns [test-results]
  (let [{:keys [started-at]}  (first test-results)
        {:keys [finished-at]} (last test-results)]
    (misc/duration-between started-at finished-at)))

(defn duration-per-namespace [report-map]
  (->> report-map
       :results
       (map #(update % 1 duration-of-tests-in-ns))
       (map (partial zipmap [:ns :duration]))))

(defn top-slowest-tests [report-map n]
  )
