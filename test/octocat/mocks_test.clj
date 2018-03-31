(ns octocat.mocks-test
  (:require [midje.sweet :refer :all]))

(defn an-impure-function [x]
  {:message "I'm really impure!"})

(defn incredible-stuff [x]
  (an-impure-function x))

(facts "about prerequisits"

       (fact "this one is mistakenly mocked out"
             (incredible-stuff {:first-name "John"}) => {:message "Hello John!"}
             (provided
              (an-impure-function {:first-name "John" :last-name "Doe"})
              => {:message "Hello John"}))

       (future-fact "one day this will be real"
                    (still-more-incredible) => irrelevant))
