(ns midje-nrepl.middlewares.fake
  "Fake middleware for testing purposes.")

(defn handle-greeting
  [message]
  (assoc message :greeting "Hello!"))
