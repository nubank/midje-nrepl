(ns midje-nrepl.misc-test
  (:require [midje-nrepl.misc :as misc]
            [midje.sweet :refer :all]))

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
                {:ns "octocat.arithmetic-test"}                                         {:kind keyword}                             {}))
