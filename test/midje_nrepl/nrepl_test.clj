(ns midje-nrepl.nrepl-test
  (:require [clojure.tools.nrepl.middleware :as middleware]
            [matcher-combinators.midje :refer [match]]
            [midje-nrepl.nrepl :refer [defmiddleware]]
            [midje.sweet :refer :all]))

(defmiddleware wrap-greeting
  {:expects  #{}
   :requires #{}
   :handles  {"greeting"
              {:doc "Politely greets the user."}}}
  'midje-nrepl.middlewares.fake/handle-greeting)

(def fake-handler #(assoc % :something :yeah))

(facts "about defining middlewares"

       (fact "the middleware contains a descriptor assoc'ed into its meta"
             (meta #'wrap-greeting)
             => (match {::middleware/descriptor
                        {:handles {"greeting"
                                   {:doc "Politely greets the user."}}}}))

       (fact "when the middleware is called with a message that doesn't match its op,
it simply calls the higher handler"
             (-> (wrap-greeting fake-handler)
                 (apply [{:op "eval" :code "(+ 1 2)"}]))
             =>                 {:op        "eval"
                                 :code      "(+ 1 2)"
                                 :something :yeah})

       (fact "when the message's op matches the middleware's op,
it replies to the message"
             (-> (wrap-greeting fake-handler)
                 (apply [{:op "greeting"}]))
             => {:op       "greeting"
                 :greeting "Hello!"}))
