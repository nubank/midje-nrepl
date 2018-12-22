(ns midje-nrepl.misc-test
  (:require [midje-nrepl.misc :as misc]
            [midje.sweet :refer :all])
  (:import [java.time Duration LocalDateTime]))

(facts "about miscellaneous functions"

       (tabular (fact "returns true if the dependency is in the current project's classpath or false otherwise"
                      (misc/dependency-in-classpath? ?dependency) => ?result)
                ?dependency ?result
                "clojure"    true
                "cider-nrepl"    true
                "refactor-nrepl"    true
                "midje"    true
                "amazonica"   false
                "bouncycastle"   false)

       (fact "returns a `java.time.LocalDateTime` representing the current date and time"
             (misc/now)
             => #(instance? LocalDateTime %))

       (fact "returns a `java.time.Duration` representing the duration between the two temporal objects"
             (misc/duration-between (misc/now) (misc/now))
             => #(instance? Duration %)))
