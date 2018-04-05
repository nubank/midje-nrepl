(ns midje-nrepl.nrepl
  (:require [clojure.tools.nrepl.middleware :refer [set-descriptor!]]))

(defn- get-handler-function [handler-symbol]
  (require (symbol (namespace handler-symbol)))
  (resolve handler-symbol))

(defn- call-handler [handler-symbol message]
  (let [handler @(get-handler-function handler-symbol)]
    (apply handler [message])))

(defn- middleware [{:keys [handles]} handler-symbol higher-handler]
  (let [supported-ops (set (keys handles))]
    (fn [{:keys [op] :as message}]
      (if (supported-ops op)
        (call-handler handler-symbol message)
        (higher-handler message)))))

(defmacro defmiddleware [name descriptor handler-symbol]
  `(do
     (defn ~name [handler#]
       (#'middleware ~descriptor ~handler-symbol handler#))
     (set-descriptor! (var ~name) ~descriptor)))
