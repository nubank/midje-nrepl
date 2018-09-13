(ns midje-nrepl.middleware.inhibit-tests-test
  (:require [clojure.test :refer [*load-tests*]]
            [clojure.tools.nrepl.misc :refer [response-for]]
            [clojure.tools.nrepl.transport :as transport]
            [matcher-combinators.midje :refer [match]]
            [midje-nrepl.middleware.inhibit-tests :as inhibit-tests]
            [midje-nrepl.reporter :as reporter :refer [with-in-memory-reporter]]
            [midje.sweet :refer :all]))

(def eval-message {:op "eval"
                   :code "(+ 1 2)"
                   :session (atom {#'*ns* "octocat.arithmetic-test"})})

(defn fake-interruptible-eval-handler [{:keys [code] :as message}]
  (assoc message :value "3"))

(facts "about handling messages with an `eval` op"

       (fact "delegates the incoming message to the base handler"
             (inhibit-tests/handle-inhibit-tests eval-message fake-interruptible-eval-handler)
             => (match {:value "3"}))

       (fact "assoc's the var `*load-test*` with a false value into the session atom"
             (-> (inhibit-tests/handle-inhibit-tests eval-message fake-interruptible-eval-handler)
                 :session
                 deref)
             => {#'*ns*         "octocat.arithmetic-test"
                 #'*load-tests* false})

       (fact "clients can override the above behavior by sending the parameter `load-tests?` in the message"
             (-> (assoc eval-message :load-tests? "true")
                 (inhibit-tests/handle-inhibit-tests fake-interruptible-eval-handler)
                 :session
                 deref)
             => {#'*ns*         "octocat.arithmetic-test"
                 #'*load-tests* true})

       (fact "clients can set the parameter `load-tests?` to false too"
             (-> (assoc eval-message :load-tests? "false")
                 (inhibit-tests/handle-inhibit-tests fake-interruptible-eval-handler)
                 :session
                 deref)
             => {#'*ns*         "octocat.arithmetic-test"
                 #'*load-tests* false}))

(def warm-ast-cache-message {:op "warm-ast-cache"})

(defn fake-warm-ast-cache-handler [{:keys [transport] :as message}]
  (assoc message
         :test-report (with-in-memory-reporter 'octocat.arithmetic-test
                        (require 'octocat.arithmetic-test :reload)
                        (transport/send transport (response-for message :status :done)))))

(facts "about handling messages with a `warm-ast-cache` op"

       (fact "delegates to the base handler without running tests"
             (inhibit-tests/handle-inhibit-tests (assoc warm-ast-cache-message :transport ..transport..) fake-warm-ast-cache-handler)
             => (match {:op "warm-ast-cache"
                        :test-report reporter/no-tests})
             (provided
              (transport/send ..transport.. anything) => irrelevant)))
