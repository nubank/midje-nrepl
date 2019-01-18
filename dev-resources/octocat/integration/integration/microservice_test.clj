(ns integration.microservice-test
  (:require [midje.sweet :refer :all]
            [selvage.flow :refer [*world* flow]]))

(def users-service (atom {}))

(def counter (atom 0))

(defn create-user! [user]
  (reset! counter 0)
  (fn [world]
    (let [user-id (get user :id)]
      (swap! users-service assoc user-id user)
      (assoc world :user-id user-id))))

(defn get-user-by-id [user-id]
  (swap! counter inc)
  (if (< @counter 3)
    nil
    (get @users-service user-id)))

(def john-doe {:id 1 :first-name "John" :last-name "Doe"})

(def linus-torvalds  {:id 2 :first-name "Linus" :last-name "Torvalds"})

(flow "creates and retrieves an user"
      (create-user! john-doe)
      (fact "the user has been created"
            (get-user-by-id (*world* :user-id)) => john-doe)

      (create-user! linus-torvalds)
      (fact "this is the wrong user!"
            (get-user-by-id (*world* :user-id)) => {:id 2
                                                    :first-name "Richard"
                                                    :last-name "Stallman"}))
