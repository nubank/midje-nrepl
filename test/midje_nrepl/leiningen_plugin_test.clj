(ns midje-nrepl.leiningen-plugin-test
  (:require [matcher-combinators.matchers :as m]
            [matcher-combinators.midje :refer [match]]
            [midje-nrepl.leiningen-plugin :as plugin]
            [midje-nrepl.middleware.version :as version]
            [midje-nrepl.nrepl :as midje-nrepl]
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

(def project-with-repl-options (assoc basic-project
                                      :repl-options {:host    "0.0.0.0"
                                                     :port    4001
                                                     :timeout 40000}))

(def project-with-repl-options-and-nrepl-middleware (-> project-with-repl-options
                                                         (assoc :dependencies '([org.clojure/clojure "1.9.0"]))
                                                         (assoc-in [:repl-options :nrepl-middleware]
                                                                   [`identity])))

(def deps-with-only-midje-nrepl [['midje-nrepl "1.0.0"]])

(def deps-with-clojure-and-midje-nrepl [['org.clojure/clojure "1.9.0"]
                                        ['midje-nrepl "1.0.0"]])

(def midje-nrepl-middleware (m/in-any-order midje-nrepl/middleware))

(def midje-nrepl-middleware-along-with-another-middleware (m/in-any-order (cons `identity midje-nrepl/middleware)))

(def augmented-basic-project (assoc basic-project
                                    :dependencies deps-with-only-midje-nrepl
                                    :repl-options
                                    {:nrepl-middleware midje-nrepl-middleware}))

(def augmented-project-with-repl-options (-> project-with-repl-options
                                             (assoc                                     :dependencies deps-with-only-midje-nrepl)
                                             (assoc-in [:repl-options :nrepl-middleware] midje-nrepl-middleware)))

(def augmented-project-with-repl-options-and-nrepl-middleware (-> project-with-repl-options-and-nrepl-middleware
                                                                  (assoc :dependencies deps-with-clojure-and-midje-nrepl)
                                                                  (assoc-in [:repl-options :nrepl-middleware] midje-nrepl-middleware-along-with-another-middleware)))

(facts "about the Leiningen plugin"
       (against-background
        (version/get-current-version) => "1.0.0")

       (tabular (fact "augments the project map by injecting midje-nrepl's middleware"
                      (plugin/middleware ?project)
                      => (match (m/equals ?augmented-project)))
                ?project                                         ?augmented-project
                basic-project                                    augmented-basic-project
                project-with-repl-options                        augmented-project-with-repl-options
                project-with-repl-options-and-nrepl-middleware  augmented-project-with-repl-options-and-nrepl-middleware))
