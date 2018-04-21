(ns midje-nrepl.nrepl-test
  (:require [clojure.tools.nrepl.middleware :as middleware]
            [clojure.tools.nrepl.transport :as transport]
            [matcher-combinators.midje :refer [match]]
            [midje-nrepl.nrepl :refer [defmiddleware]]
            [midje.sweet :refer :all]))

(defmiddleware wrap-greeting
  {:expects  #{}
   :requires #{}
   :handles  {"greeting"
              {:doc "Sends a generic greeting to the user."}
              "personal-greeting"
              {:doc      "Sends a personal greeting to the user."
               :requires {"first-name" "The first name of the user."
                          "last-name"  "The last name of the user."}}}}
  'midje-nrepl.middleware.fake/handle-greeting)

(def fake-handler #(assoc % :something :yeah))

(facts "about defining middleware"

       (fact "the middleware contains a descriptor assoc'ed into its meta"
             (meta #'wrap-greeting)
             => (match {::middleware/descriptor
                        {:handles {"greeting"
                                   {:doc "Sends a generic greeting to the user."}}}}))

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
                 :greeting "Hello!"})

       (tabular (fact "returns an error when required parameters are missing"
                      (-> (wrap-greeting identity)
                          (apply [(merge {:op "personal-greeting" :transport ..transport..} ?message)])) => irrelevant
                      (provided
                       (transport/send ..transport.. {:status ?status}) => irrelevant))
                ?message              ?status
                {:first-name "John"}  #{:error :no-last-name}
                {:last-name "Doe"}    #{:error :no-first-name}
                {}                    #{:error :no-first-name :no-last-name})

       (fact "calls the middleware normally when all required parameters are provided"
             (-> (wrap-greeting fake-handler)
                 (apply [{:op "personal-greeting" :first-name "John" :last-name "Doe"}]))
             => (match {:greeting "Hello John Doe!"})))
