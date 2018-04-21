(ns midje-nrepl.middleware.fake
  "Fake middleware for testing purposes.")

(defn handle-greeting
  [{:keys [op first-name last-name] :as message}]
  (case op
    "greeting"          (assoc message :greeting "Hello!")
    "personal-greeting" (assoc message :greeting (format "Hello %s %s!" first-name last-name))))
