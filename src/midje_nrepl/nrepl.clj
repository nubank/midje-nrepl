(ns midje-nrepl.nrepl
  (:require [clojure.set :as set]
            [clojure.tools.nrepl.middleware :refer [set-descriptor!]]
            [clojure.tools.nrepl.middleware.load-file :as load-file]
            [clojure.tools.nrepl.middleware.interruptible-eval :as eval]
            [clojure.tools.nrepl.misc :refer [response-for]]
            [clojure.tools.nrepl.transport :as transport]))

(defn- greatest-arity-of [handler-var]
  {:post [(or (= % 1) (= % 2))]}
  (->> handler-var
       meta
       :arglists
       (map count)
       (apply max)))

(defn- try-to-call-handler [delayed-handler higher-handler message]
  (let [args (if (= 1 (greatest-arity-of @delayed-handler))
               [message]
               [message higher-handler])]
    (apply @@delayed-handler args)))

(defn- explain-missing-parameters [message op-descriptor]
  (let [key-set         #(->> % keys (map keyword) set)
        required-params (key-set (op-descriptor :requires))
        actual-params   (key-set message)]
    (->> (set/difference required-params actual-params)
         (map (comp keyword #(str "no-" %) name)))))

(defn- call-handler [delayed-handler higher-handler {:keys [transport] :as message} op-descriptor]
  (let [missing-params (explain-missing-parameters message op-descriptor)]
    (if (seq missing-params)
      (transport/send transport (response-for message :status (set/union #{:done :error} missing-params)))
      (try-to-call-handler delayed-handler higher-handler message))))

(defn make-middleware [descriptor delayed-handler higher-handler]
  (fn [{:keys [op] :as message}]
    (if-let [op-descriptor (get-in descriptor [:handles (name op)])]
      (call-handler delayed-handler higher-handler message op-descriptor)
      (higher-handler message))))

(defn delayed-handler-var [handler-symbol]
  (delay   (require (symbol (namespace handler-symbol)))
           (resolve handler-symbol)))

(defmacro defmiddleware [name descriptor handler-symbol]
  `(let [delayed-handler# (delayed-handler-var ~handler-symbol)]
     (defn ~name [handler#]
       (make-middleware ~descriptor delayed-handler# handler#))
     (set-descriptor! (var ~name) ~descriptor)))

(defmiddleware wrap-format
  {:expects  #{}
   :requires #{}
   :handles  {"midje-format-tabular"
              {:requires {"code" "The tabular sexpr to be formatted"}}}}
  'midje-nrepl.middleware.format/handle-format)

(defmiddleware wrap-load
  {:expects #{#'eval/interruptible-eval}
   :handles {"eval"
             {:doc "Delegates to the next `eval` middleware, by preventing Midje facts from being run"}}}
  'midje-nrepl.middleware.load/handle-load)

(defmiddleware wrap-test
  {:expects  #{}
   :requires #{}
   :handles  {"midje-test-ns"
              {:doc      "Runs all Midje tests in the namespace."
               :requires {"ns" "A string indicating the namespace containing the tests to be run."}}
              "midje-test"
              {:doc      "Runs a given Midje test (either an individual fact or facts)."
               :requires {"ns"         "The namespace in which the fact(s) sent through `test-forms` should be evaluated."
                          "test-forms" "The fact(s) to be run."}}
              "midje-retest"
              {:doc "Re-runs the tests that didn't pass in the last execution."}
              "midje-test-stacktrace"
              {:doc      "Returns the stacktrace of a given erring test. Returns the status `no-stacktrace` if there is no stacktrace for the specified test."
               :requires {"ns"       "A string indicating the namespace of the erring test."
                          "index"    "An integer indicating the index of the erring test in question."
                          "print-fn" "Fully qualified name of a print function that will be used to print stacktraces."}}}}
  'midje-nrepl.middleware.test/handle-test)

(defmiddleware wrap-version
  {:expects  #{}
   :requires #{}
   :handles  {"midje-nrepl-version"
              {:doc "Provides information about midje-nrepl's current version."}}}
  'midje-nrepl.middleware.version/handle-version)

(def middleware `[wrap-format
                  wrap-load
                  wrap-test
                  wrap-version])
