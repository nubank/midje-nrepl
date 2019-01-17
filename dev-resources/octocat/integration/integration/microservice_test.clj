(ns integration.microservice-test
  (:require [midje.sweet :refer :all]
            [selvage.flow :refer [*world* flow]]))

(def users-service (atom {}))

(def counter (atom 0))

(defn create-user [user]
  (fn [world]
    (let [user-id 1]
      (swap! users-service assoc user-id user)
      (assoc world :user-id user-id))))

(defn get-user-by-id [user-id]
  (swap! counter inc)
  (if (< @counter 3)
    nil
    (get @users-service user-id)))

(defn reset-counter! [world]
  (reset! counter 0)
  world)

(def john-doe {:first-name "John" :last-name "Doe"})

(flow "creates and retrieves an user"
      (create-user john-doe)
      (fact "the user has been created"
            (get-user-by-id (*world* :user-id)) => john-doe)
      reset-counter!)
