(ns midje-nrepl.coverage-test
  (:require [cloverage.dependency :as dependency]
            [matcher-combinators.matchers :as m]
            [matcher-combinators.midje :refer [match]]
            [midje-nrepl.coverage :as coverage]
            [midje-nrepl.runner :as runner]
            [midje.sweet :refer :all]))

(def noop-logger (constantly nil))

(def source-namespaces ['midje-nrepl.middleware.format
                        'midje-nrepl.middleware.inhibit-tests
                        'midje-nrepl.middleware.test])

(facts "about instrumenting namespaces"
       (against-background
        (#'coverage/instrument-namespace 'midje-nrepl.middleware.format) => :success
        (#'coverage/instrument-namespace 'midje-nrepl.middleware.inhibit-tests) => :success
        (#'coverage/instrument-namespace 'midje-nrepl.middleware.test) => :success)

       (fact "returns `:success` when all namespaces are properly instrumented and logs the relevant events"
             (coverage/instrument-namespaces source-namespaces (coverage/wrap-logger noop-logger)) => :success
             (provided
              (noop-logger :info "Loading namespaces...") => irrelevant
              (noop-logger :info "Instrumenting midje-nrepl.middleware.format...") => irrelevant
              (noop-logger :info "Instrumenting midje-nrepl.middleware.inhibit-tests...") => irrelevant
              (noop-logger :info "Instrumenting midje-nrepl.middleware.test...") => irrelevant
              (noop-logger :info "All namespaces (3) were successfully instrumented.") => irrelevant))

       (fact "returns `:error` and logs an appropriate message when there is a cyclic dependency among the namespaces"
             (coverage/instrument-namespaces source-namespaces (coverage/wrap-logger noop-logger)) => :error
             (provided
              (dependency/in-dependency-order source-namespaces) => []
              (noop-logger :info "Loading namespaces...") => irrelevant
              (noop-logger :error "Could not instrument namespaces: there is a cyclic dependency.") => irrelevant))

       (fact "returns `:error` when some namespace cannot be instrumented and logs the relevant events"
             (coverage/instrument-namespaces source-namespaces (coverage/wrap-logger noop-logger)) => :error
             (provided
              (#'coverage/instrument-namespace 'midje-nrepl.middleware.inhibit-tests) =throws=> (ex-info "Boom!" {:reason :some-failure})
              (noop-logger :info "Loading namespaces...") => irrelevant
              (noop-logger :info "Instrumenting midje-nrepl.middleware.format...") => irrelevant
              (noop-logger :info "Instrumenting midje-nrepl.middleware.inhibit-tests...") => irrelevant
              (noop-logger :info "Instrumenting midje-nrepl.middleware.test...") => irrelevant
              (noop-logger :error "Could not instrument namespace midje-nrepl.middleware.inhibit-tests. clojure.lang.ExceptionInfo: Boom! {:reason :some-failure}.") => irrelevant
              (noop-logger :error "Could not capture code coverage due to previous problems.") => irrelevant)))

(def logging-messages (atom []))

(defn coverage-logger [level message]
  (swap! logging-messages conj {:level level :message message}))

(facts "about running tests and collecting code coverage information"
       (against-background
        (before :contents (reset! logging-messages [])))

       (fact "runs tests by collecting coverage information"
             ((coverage/code-coverage runner/run-tests-in-ns) {:ns                'midje-nrepl.middleware.version-test
                                                               :source-namespaces ['midje-nrepl.middleware.version]
                                                               :coverage-logger   coverage-logger})
             => (match {:coverage {:summary {:percent-of-forms   "100%"
                                             :percent-of-lines   "100%"
                                             :coverage-threshold 50
                                             :result             :acceptable-coverage}}}))

       (fact "logs some events as they happen"
             @logging-messages
             => (match [{:level :info :message "Loading namespaces..."}
                        {:level :info :message "Instrumenting midje-nrepl.middleware.version..."}
                        {:level :info :message "All namespaces (1) were successfully instrumented."}]))

       (fact "runs tests but do not assoc the coverage report when namespaces cannot be properly instrumented"
             (keys ((coverage/code-coverage runner/run-tests-in-ns) {:ns                'midje-nrepl.middleware.version-test
                                                                     :source-namespaces ['midje-nrepl.middleware.version]}))
             => (match (m/in-any-order [:results :summary]))
             (provided
              (coverage/instrument-namespaces ['midje-nrepl.middleware.version] anything) => :error)))
