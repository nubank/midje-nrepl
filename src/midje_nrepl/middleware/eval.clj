(ns midje-nrepl.middleware.eval
  (:require [clojure.test :refer [*load-tests*]]))

(defn handle-eval [message base-handler]
  (let [load-tests? (boolean (message :load-tests?))]
    (update message :session
            swap! assoc #'*load-tests* load-tests?))
  (base-handler message))
