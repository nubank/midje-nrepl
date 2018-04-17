(ns midje-nrepl.nrepl
  (:require [clojure.set :as set]
            [clojure.tools.nrepl.middleware :refer [set-descriptor!]]
            [clojure.tools.nrepl.misc :refer [response-for]]
            [clojure.tools.nrepl.transport :as transport]))

(defn- explain-missing-parameters [message op-descriptor]
  (let [key-set         #(->> % keys (map keyword) set)
        required-params (key-set (op-descriptor :requires))
        actual-params   (key-set message)]
    (->> (set/difference required-params actual-params)
         (map (comp keyword #(str "no-" %) name)))))

(defn- call-handler [delayed-handler {:keys [transport] :as message} op-descriptor]
  (let [missing-params (explain-missing-parameters message op-descriptor)]
    (if (seq missing-params)
      (transport/send transport (response-for message :status (set (cons :error missing-params))))
      (apply @delayed-handler [message]))))

(defn middleware [descriptor delayed-handler higher-handler]
  (fn [{:keys [op] :as message}]
    (if-let [op-descriptor (get-in descriptor [:handles (name op)])]
      (call-handler delayed-handler message op-descriptor)
      (higher-handler message))))

(defn delayed-handler-function [handler-symbol]
  (delay   (require (symbol (namespace handler-symbol)))
           (deref (resolve handler-symbol))))

(defmacro defmiddleware [name descriptor handler-symbol]
  `(let [delayed-handler# (delayed-handler-function ~handler-symbol)]
     (defn ~name [handler#]
       (middleware ~descriptor delayed-handler# handler#))
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
