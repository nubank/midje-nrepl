(ns midje-nrepl.middleware.format-test
  (:require [matcher-combinators.midje :refer [match]]
            [midje-nrepl.formatter :as formatter]
            [midje-nrepl.middleware.format :as format]
            [midje.sweet :refer :all]
            [nrepl.transport :as transport]))

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
              (transport/send ..transport.. (match {:status #{:done}})) => irrelevant))

       (fact "when the formatter throws a known exception, responds with a meaningful error to the client"
             (format/handle-format {:transport ..transport..
                                    :code      "(+ 1 2)"}) => irrelevant
             (provided
              (transport/send ..transport.. (match {:error-message string?
                                                    :status        #{:done :error :no-tabular}}))   => irrelevant))

       (fact "when the formatter throws an unknown exception, propagates it"
             (format/handle-format {:transport ..transport..
                                    :code      ..code..}) => (throws Exception #"Boom!")
             (provided
              (formatter/format-tabular ..code..) =throws=> (Exception. "Boom!"))))
