(ns midje-nrepl.middleware.format-test
  (:require [clojure.tools.nrepl.transport :as transport]
            [matcher-combinators.midje :refer [match]]
            [midje-nrepl.middleware.format :as format]
            [midje.sweet :refer :all]))

(def basic-tabular "(tabular (fact (+ ?x ?y) => ?z)
  ?x ?y ?z
  1000 15 1015
  2 1 3)")

(def formatted-basic-tabular "(tabular (fact (+ ?x ?y) => ?z)
    ?x ?y   ?z
  1000 15 1015
     2  1    3)")

(facts "about the format handler"

       (fact "takes a tabular sexpr, formats it and returns the formatted sexpr to the client"
             (format/handle-format {:transport ..transport..
                                    :code      basic-tabular})
             => irrelevant
             (provided
              (transport/send ..transport.. (match {:formatted-code formatted-basic-tabular})) => irrelevant
              (transport/send ..transport.. (match {:status #{:done}})) => irrelevant)))