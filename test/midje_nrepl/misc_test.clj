(ns midje-nrepl.misc-test
  (:require [midje-nrepl.misc :as misc]
            [midje.sweet :refer :all])
  (:import [java.time Duration Instant]))

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

       (tabular (fact "parses options according to the provided parsers map;
       removes keys that aren't declared in the parsers map"
                      (misc/parse-options ?options ?parsers-map) => ?result)
                ?options                                            ?parsers-map                        ?result
                {:ns "octocat.arithmetic-test"}                                            {:ns symbol} {:ns 'octocat.arithmetic-test}
                {:x "12" :y "true"} {:x #(Integer/parseInt %) :y #(Boolean/parseBoolean %)}                {:x 12 :y true}
                {:test-paths ["test"]}                                  {:test-paths identity}         {:test-paths ["test"]}
                {:ns "octocat.arithmetic-test"}                                         {:kind keyword}                             {})

       (fact "returns a `java.time.Instant` representing the current instant"
             (misc/now)
             => #(instance? Instant %))

       (fact "returns a `java.time.Duration` representing the duration between the two temporal objects"
             (misc/duration-between (misc/now) (misc/now))
             => #(instance? Duration %))

       (tabular (fact "formats the supplied decimal number as a string"
                      (misc/format-decimal ?value) => ?result)
                ?value ?result
                15.142 "15.14"
                20.00    "20"
                37.088 "37.09")

       (fact "returns the decimal number as a string representing a percent value"
             (misc/percent 28.016) => "28.02%"))
