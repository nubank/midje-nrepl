(ns midje-nrepl.nrepl
  (:require [clojure.tools.nrepl.middleware :refer [set-descriptor!]]
            [clojure.tools.nrepl.server :as nrepl.server]))

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

(defmiddleware wrap-test
  {:expects  #{}
   :requires #{}
   :handles  {"midje-test-ns"
              {:doc      "Runs all Midje tests in the namespace."
               :requires {"ns" "A string indicating the namespace containing the tests to be run."}
               :optional {}}}}
  'midje-nrepl.middlewares.test/handle-test)

(defmiddleware wrap-version
  {:expects  #{}
   :requires #{}
   :handles  {"version"
              {:doc "Provides information about midje-nrepl's current version."}}}
  'midje-nrepl.middlewares.version/handle-version)

(def middlewares `[wrap-test
                   wrap-version])
