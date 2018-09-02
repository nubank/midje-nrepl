(ns midje-nrepl.middleware.load
  (:require [clojure.test :refer [*load-tests*]]))

(defn handle-load [message base-handler]
  (update message :session
          swap! assoc #'*load-tests* false)
  (base-handler message))
