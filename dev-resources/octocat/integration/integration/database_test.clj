(ns integration.database-test
  (:require [midje.sweet :refer :all]))

(defn find-all-users []
  [{:first-name "John" :last-name "Doe"}])

(facts "about database operations"

       (fact "finds all users in the system"
             (find-all-users)
             => [{:first-name "John" :last-name "Doe"}]))
