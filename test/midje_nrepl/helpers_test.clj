(ns midje-nrepl.helpers-test
  (:require [midje-nrepl.helpers :refer :all]
            [midje.sweet :refer :all]))

(tabular (fact "returns true if the dependency is in the current project's classpath or false otherwise"
               (dep-in-classpath? ?dependency) => ?result)
         ?dependency ?result
         "clojure" true
         "cider-nrepl" true
         "refactor-nrepl" true
         "midje" true
         "amazonica" false
         "bouncycastle" false)
