(ns octocat.mocks-test
  (:require [midje.emission.state :refer [with-isolated-output-counters]]
            [midje.sweet :refer :all]))

(defn an-impure-function [_]
  {:message "I'm really impure!"})

(defn incredible-stuff [x]
  (an-impure-function x))

(with-isolated-output-counters

  (facts "about prerequisits" :mark3

         (fact "this one is mistakenly mocked out"
               (incredible-stuff {:first-name "John"}) => {:message "Hello John!"}
               (provided
                (an-impure-function {:first-name "John" :last-name "Doe"})
                => {:message "Hello John"}))))
