(ns integration.format-test
  (:require [integration.helpers :refer [send-message]]
            [matcher-combinators.matchers :as m]
            [matcher-combinators.midje :refer [match]]
            [midje.sweet :refer :all]))

(def tabular1 "(tabular (fact (+ ?x ?y) => ?z)
  ?x ?y ?z
  1000 15 1015
  2 1 3)")

(def formatted-tabular1 "(tabular (fact (+ ?x ?y) => ?z)
    ?x ?y   ?z
  1000 15 1015
     2  1    3)")

(facts "about formatting tabular facts"

       (fact "formats the tabular fact sent in the message"
             (send-message {:op   "midje-format-tabular"
                            :code tabular1})
             => (match (list {:formatted-code formatted-tabular1}
                             {:status ["done"]})))

       (fact "when the code is missing in the message, the middleware returns an error"
             (first (send-message {:op "midje-format-tabular"}))
             => (match {:status (m/in-any-order ["done" "error" "no-code"])}))

       (fact "when the user sends invalid sexprs (like a non-tabular one), the middleware returns meaningful error responses"
             (first (send-message {:op   "midje-format-tabular"
                                   :code "(fact 1 => 1)"}))
             => (match {:error-message "Sexpr must be a tabular fact"
                        :status        (m/in-any-order ["done" "error" "no-tabular"])})))
