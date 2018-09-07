(ns midje-nrepl.middleware.eval
  (:require [clojure.test :refer [*load-tests*]]))

(defn handle-eval [message base-handler]
  (update message :session
          swap! assoc #'*load-tests* false)
  (base-handler message))
