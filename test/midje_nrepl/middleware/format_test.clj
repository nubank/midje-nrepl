(ns midje-nrepl.middleware.format-test
  (:require [midje-nrepl.middleware.format :as format]
            [matcher-combinators.midje :refer [match]]
            [midje.sweet :refer :all]
            [clojure.tools.nrepl.transport :as transport]))

(def tabular1 "(tabular (fact (+ ?x ?y) => ?z)
  ?x ?y ?z
  10 15 25
  2 1 3)")

(def formatted-tabular1 "(tabular (fact (+ ?x ?y) => ?z)
    ?x ?y ?z
  1000 15 1015
     2  1    3 )")

(facts "about the format handler"

       (fact "takes a tabular sexpr, formats it and returns the formatted sexpr to the client"
             (format/handle-format {:op        "midje-format"
                                    :transport ..transport..
                                    :code      tabular1})
             => irrelevant
             (provided
              (transport/send ..transport.. (match {:code formatted-tabular1})) => irrelevant
              (transport/send ..transport.. (match {:status #{:done}})) => irrelevant)))
