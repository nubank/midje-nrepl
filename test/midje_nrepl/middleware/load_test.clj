(ns midje-nrepl.middleware.load-test
  (:require [clojure.test :refer [*load-tests*]]
            [matcher-combinators.midje :refer [match]]
            [midje-nrepl.middleware.load :as load]
            [midje.sweet :refer :all]))

(def incoming-message {:session (atom {#'*ns* "octocat.arithmetic-test"})})

(defn base-handler [message]
  (assoc message ::base-handler-called? true))

(facts "about the load middleware"
       (let [resulting-message (load/handle-load incoming-message base-handler)]

         (fact "the load middleware delegates to the base handler by keeping the session as an atom"
               resulting-message => (match {::base-handler-called? true
                                            :session               (partial instance? clojure.lang.Atom)})) (fact "the load middleware assoc's the `*load-tests*` var into the session"
                                                                                                                  @(resulting-message :session)
                                                                                                                  => (match {#'*ns*         "octocat.arithmetic-test"
                                                                                                                             #'*load-tests* false}))))
