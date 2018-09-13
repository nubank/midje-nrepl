(ns midje-nrepl.middleware.inhibit-tests-test
  (:require [clojure.test :refer [*load-tests*]]
            [matcher-combinators.midje :refer [match]]
            [midje-nrepl.middleware.inhibit-tests :as eval]
            [midje.sweet :refer :all]))

(def eval-message {:op "eval"
                   :code "(+ 1 2)"
                   :session (atom {#'*ns* "octocat.arithmetic-test"})})

(defn fake-interruptible-eval-handler [{:keys [code] :as message}]
  (assoc message :value "3"))

(facts "about handling messages with an `eval` op"

       (fact "delegates the incoming message to the base handler"
             (eval/handle-inhibit-tests eval-message fake-interruptible-eval-handler)
             => (match {:value "3"}))

       (fact "assoc's the var `*load-test*` with a false value into the session atom"
             (-> (eval/handle-inhibit-tests eval-message fake-interruptible-eval-handler)
                 :session
                 deref)
             => {#'*ns*         "octocat.arithmetic-test"
                 #'*load-tests* false})

       (fact "clients can override the above behavior by sending the parameter `load-tests?` in the message"
             (-> (assoc eval-message :load-tests? "true")
                 (eval/handle-inhibit-tests fake-interruptible-eval-handler)
                 :session
                 deref)
             => {#'*ns*         "octocat.arithmetic-test"
                 #'*load-tests* true})

       (fact "clients can set the parameter `load-tests?` to false too"
             (-> (assoc eval-message :load-tests? "false")
                 (eval/handle-inhibit-tests fake-interruptible-eval-handler)
                 :session
                 deref)
             => {#'*ns*         "octocat.arithmetic-test"
                 #'*load-tests* false}))

(def warm-ast-cache-message {:op "warm-ast-cache"})
