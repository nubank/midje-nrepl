(ns midje-nrepl.middleware.load
  (:require [clojure.test :refer [*load-tests*]]))

(defn handle-load [message higher-handler]
  (binding [*load-tests* false]
    (higher-handler message)))
