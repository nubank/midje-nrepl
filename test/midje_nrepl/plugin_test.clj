(ns midje-nrepl.plugin-test
  (:require [matcher-combinators.matchers :as m]
            [matcher-combinators.midje :refer [match]]
            [midje-nrepl.middleware.version :as version]
            [midje-nrepl.nrepl :as midje-nrepl]
            [midje-nrepl.plugin :as plugin]
            [midje.sweet :refer :all]))

(def basic-project {:description "FIXME: write description"
                    :compile-path
                    "/home/john-doe/projects/octocat/target/classes"
                    :deploy-repositories
                    [["clojars"
                      {:url      "https://clojars.org/repo/"
                       :password :gpg
                       :username :gpg}]]
                    :group       "octocat"
                    :resource-paths
                    '("/home/john-doe/projects/octocat/dev-resources"
                      "/home/john-doe/projects/octocat/resources")
                    :test-paths
                    '("/home/john-doe/projects/octocat/test")})

(def project-with-conflicting-version-of-clojure-tools-namespace
  (assoc basic-project :dependencies [['org.clojure/tools.namespace "0.2.11"]]))

(def project-with-repl-options (assoc basic-project
                                      :repl-options {:host    "0.0.0.0"
                                                     :port    4001
                                                     :timeout 40000}))

(def project-with-repl-options-and-nrepl-middleware (-> project-with-repl-options
                                                        (assoc :dependencies '([org.clojure/clojure "1.9.0"]))
                                                        (assoc-in [:repl-options :nrepl-middleware]
                                                                  [`identity])))

(def deps-with-midje-nrepl-and-clojure-tools-namespace [['nubank/midje-nrepl "1.0.0"]
                                                        (m/equals ['org.clojure/tools.namespace #"^0\.3"])])

(def deps-with-clojure-midje-nrepl-and-clojure-tools-namespace (into [['org.clojure/clojure "1.9.0"]]
                                                                     deps-with-midje-nrepl-and-clojure-tools-namespace))

(def midje-nrepl-middleware (m/in-any-order midje-nrepl/middleware))

(def midje-nrepl-middleware-along-with-another-middleware (m/in-any-order (cons `identity midje-nrepl/middleware)))

(def augmented-basic-project (assoc basic-project
                                    :dependencies deps-with-midje-nrepl-and-clojure-tools-namespace
                                    :repl-options
                                    {:nrepl-middleware midje-nrepl-middleware}))

(def augmented-project-with-repl-options (-> project-with-repl-options
                                             (assoc                                     :dependencies deps-with-midje-nrepl-and-clojure-tools-namespace)
                                             (assoc-in [:repl-options :nrepl-middleware] midje-nrepl-middleware)))

(def augmented-project-with-repl-options-and-nrepl-middleware (-> project-with-repl-options-and-nrepl-middleware
                                                                  (assoc :dependencies deps-with-clojure-midje-nrepl-and-clojure-tools-namespace)
                                                                  (assoc-in [:repl-options :nrepl-middleware] midje-nrepl-middleware-along-with-another-middleware)))

(facts "about the Leiningen plugin"
       (against-background
        (version/get-current-version) => "1.0.0")

       (tabular (fact "augments the project map by injecting midje-nrepl's middleware"
                      (plugin/middleware ?project)
                      => (match (m/equals ?augmented-project)))
                ?project                                       ?augmented-project
                basic-project                                  augmented-basic-project
                project-with-conflicting-version-of-clojure-tools-namespace                                  augmented-basic-project
                project-with-repl-options                      augmented-project-with-repl-options
                project-with-repl-options-and-nrepl-middleware augmented-project-with-repl-options-and-nrepl-middleware))
