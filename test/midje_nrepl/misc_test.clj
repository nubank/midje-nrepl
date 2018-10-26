(ns midje-nrepl.misc-test
  (:require [midje.sweet :refer :all]
            [midje-nrepl.misc :as misc]))

(facts "about miscellaneous functions"

       (tabular (fact "returns true if the dependency is in the current project's classpath or false otherwise"
                      (misc/dependency-in-classpath? ?dependency) => ?result)
                ?dependency ?result
                "clojure"    true
                "cider-nrepl"    true
                "refactor-nrepl"    true
                "midje"    true
                "amazonica"   false
                "bouncycastle"   false))
