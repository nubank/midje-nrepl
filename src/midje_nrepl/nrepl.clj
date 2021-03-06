(ns midje-nrepl.nrepl
  (:require [cider.nrepl :as cider]
            [clojure.set :as set]
            [midje-nrepl.misc :as misc]
            [nrepl.middleware :refer [set-descriptor!]]
            [nrepl.middleware.interruptible-eval :as eval]
            [nrepl.misc :refer [response-for]]
            [nrepl.transport :as transport]))

(defn- greatest-arity-of [handler-var]
  {:post [(or (= % 1) (= % 2))]}
  (->> handler-var
       meta
       :arglists
       (map count)
       (apply max)))

(defn- try-to-call-handler [delayed-handler base-handler message]
  (let [args (if (= 1 (greatest-arity-of @delayed-handler))
               [message]
               [message base-handler])]
    (apply @@delayed-handler args)))

(defn- explain-missing-parameters [message op-descriptor]
  (let [key-set         #(->> % keys (map keyword) set)
        required-params (key-set (op-descriptor :requires))
        actual-params   (key-set message)]
    (->> (set/difference required-params actual-params)
         (map (comp keyword #(str "no-" %) name)))))

(defn- call-handler [delayed-handler base-handler {:keys [transport] :as message} op-descriptor]
  (let [missing-params (explain-missing-parameters message op-descriptor)]
    (if (seq missing-params)
      (transport/send transport (response-for message :status (set/union #{:done :error} missing-params)))
      (try-to-call-handler delayed-handler base-handler message))))

(defn make-middleware [descriptor delayed-handler base-handler]
  (fn [{:keys [op] :as message}]
    (if-let [op-descriptor (get-in descriptor [:handles (name op)])]
      (call-handler delayed-handler base-handler message op-descriptor)
      (base-handler message))))

(defn delayed-handler-var [handler-symbol]
  (delay   (require (symbol (namespace handler-symbol)))
           (resolve handler-symbol)))

(defmacro defmiddleware [name descriptor handler-symbol]
  `(let [delayed-handler# (delayed-handler-var ~handler-symbol)]
     (defn ~name [handler#]
       (make-middleware ~descriptor delayed-handler# handler#))
     (set-descriptor! (var ~name) ~descriptor)))

(defmiddleware wrap-format
  {:handles {"midje-format-tabular"
             {:doc      "Formats tabular facts."
              :requires {"code" "The tabular sexpr to be formatted."}}}}
  'midje-nrepl.middleware.format/handle-format)

(defn middleware-vars-expected-by-wrap-inhibit-tests []
  (let [middleware-vars #{#'eval/interruptible-eval
                          #'cider/wrap-refresh}]
    (if (misc/dependency-in-classpath? "refactor-nrepl")
      (set/union middleware-vars #{(resolve 'refactor-nrepl.middleware/wrap-refactor)})
      middleware-vars)))

(defmiddleware wrap-inhibit-tests
  {:expects (middleware-vars-expected-by-wrap-inhibit-tests)
   :handles
   {"eval"
    {:doc      "Delegates to `interruptible-eval` middleware, by preventing Midje facts from being run."
     :optional {"load-tests?" "If set to \"true\" any Midje fact loaded in the current operation will be run automatically (defaults to \"false\")."}}
    "warm-ast-cache"
    {:doc      "Delegates to `refactor-nrepl.middleware/wrap-refactor` middleware, by preventing Midje facts from being run."
     :optional {"load-tests?" "If set to \"true\" any Midje fact loaded in the current operation will be run automatically (defaults to \"false\")."}}
    "refresh"
    {:doc      "Delegates to `cider.nrepl/wrap-refresh` by preventing Midje facts from being run."
     :optional {"load-tests?" "If set to \"true\" any Midje fact loaded in the current operation will be run automatically (defaults to \"false\")."}}
    "refresh-all"
    {:doc      "Delegates to `cider.nrepl/wrap-refresh` by preventing Midje facts from being run."
     :optional {"load-tests?" "If set to \"true\" any Midje fact loaded in the current operation will be run automatically (defaults to \"false\")."}}}}
  'midje-nrepl.middleware.inhibit-tests/handle-inhibit-tests)

(defmiddleware wrap-test
  {:handles {"midje-test-all"
             {:doc      "Runs all Midje tests defined in the project."
              :optional {"ns-exclusions" "A list of regexes to match namespaces against. Matched namespaces will be excluded from the tests."
                         "ns-inclusions" "A list of regexes to match namespaces against. Only matched namespaces will be included in the tests."
                         "profile?"      "A boolean indicating whether the middleware should collect profiling information about tests. Defaults to false."
                         "test-paths"    "A list of test directories to find tests in. Defaults to all known test directories in the current project."}}
             "midje-test-ns"
             {:doc      "Runs all Midje tests defined in the namespace."
              :requires {"ns" "A string indicating the namespace containing the tests to be run."}}
             "midje-test"
             {:doc      "Runs only the supplied Midje test (either an individual fact or facts)."
              :requires {"ns"     "The namespace in which the fact(s) sent through `source` should be evaluated."
                         "source" "The fact(s) to be run."}
              :optional
              {"line" "The line number where the facts to be tested starts."}}
             "midje-retest"
             {:doc "Re-runs the tests that didn't pass in the last execution."}
             "midje-test-stacktrace"
             {:doc      "Returns the stacktrace of a given erring test. Returns the status `no-stacktrace` if there is no stacktrace for the specified test."
              :requires {"ns"       "A string indicating the namespace of the erring test."
                         "index"    "An integer indicating the index of the erring test in question."
                         "print-fn" "Fully qualified name of a print function that will be used to print stacktraces."}}}}
  'midje-nrepl.middleware.test/handle-test)

(defmiddleware wrap-test-info
  {:handles {"test-paths"
             {:doc "Returns a list of known test paths for the current project."}
             "test-namespaces"
             {:doc      "Returns a list of test namespaces declared within specified test paths."
              :optional {"test-paths" "A list of test paths to find namespaces in. If omitted find namespaces in all known test paths."}}}}
  'midje-nrepl.middleware.test-info/handle-test-info)

(defmiddleware wrap-version
  {:handles {"midje-nrepl-version"
             {:doc "Provides information about midje-nrepl's current version."}}}
  'midje-nrepl.middleware.version/handle-version)

(def middleware `[wrap-format
                  wrap-inhibit-tests
                  wrap-test
                  wrap-test-info
                  wrap-version])
