(ns midje-nrepl.middleware.eval-test
  (:require [clojure.test :refer [*load-tests*]]
            [matcher-combinators.midje :refer [match]]
            [midje-nrepl.middleware.eval :as eval]
            [midje.sweet :refer :all]))

(def incoming-message {:session (atom {#'*ns* "octocat.arithmetic-test"})})

(defn base-handler [message]
  (assoc message ::base-handler-called? true))

(facts "about the eval middleware"

       (fact "delegates the incoming message to the base handler"
             (eval/handle-eval incoming-message base-handler)
             => (match {::base-handler-called? true}))

       (fact "assoc's the var `*load-test*` with a false value into the session atom"
             (-> (eval/handle-eval incoming-message base-handler)
                 :session
                 deref)
             => {#'*ns* "octocat.arithmetic-test"
                 #'*load-tests* false}))
