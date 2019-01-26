(ns midje-nrepl.coverage-test
  (:require [matcher-combinators.matchers :as m]
            [matcher-combinators.midje :refer [match]]
            [midje-nrepl.coverage :as coverage]
            [midje-nrepl.runner :as runner]
            [midje.sweet :refer :all]))

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
