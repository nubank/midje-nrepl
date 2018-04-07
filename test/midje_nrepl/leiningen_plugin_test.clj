(ns midje-nrepl.leiningen-plugin-test
  (:require [matcher-combinators.matchers :as m]
            [matcher-combinators.midje :refer [match]]
            [midje-nrepl.leiningen-plugin :as plugin]
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

(def project-with-repl-options-and-nrepl-middlewares (assoc-in project-with-repl-options [:repl-options :nrepl-middleware]
                                                               [identity]))

(def middlewares [midje-nrepl/wrap-test midje-nrepl/wrap-version])

(def midje-nrepl-middlewares (m/in-any-order middlewares))

(def midje-nrepl-middlewares-along-with-another-middleware (m/in-any-order (cons identity middlewares)))

(def augmented-basic-project (assoc basic-project :repl-options
                                    {:nrepl-middleware midje-nrepl-middlewares}))

(def augmented-project-with-repl-options (assoc-in project-with-repl-options [:repl-options :nrepl-middleware] midje-nrepl-middlewares))

(def augmented-project-with-repl-options-and-nrepl-middlewares (assoc-in project-with-repl-options-and-nrepl-middlewares [:repl-options :nrepl-middleware] midje-nrepl-middlewares-along-with-another-middleware))

(facts "about the Leiningen plugin"

       (tabular (fact "augments the project map by injecting midje-nrepl's middlewares"
                      (plugin/middleware ?project)
                      => (match (m/equals ?augmented-project)))
                ?project                                         ?augmented-project
                basic-project                                    augmented-basic-project
                project-with-repl-options                        augmented-project-with-repl-options
                project-with-repl-options-and-nrepl-middlewares  augmented-project-with-repl-options-and-nrepl-middlewares  ))
