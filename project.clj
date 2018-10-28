(defproject nubank/midje-nrepl "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/tools.nrepl "0.2.13"]
                 [cider/orchard "0.3.0"]
                 [rewrite-clj "0.6.0"]]

  :profiles {:dev {:dependencies [[midje "1.9.2-alpha3"]
                                  [nubank/matcher-combinators "0.3.2"]]
                   :plugins [[lein-midje "3.2.1"]]}
             :provided {:dependencies [[cider/cider-nrepl "0.17.0"]
                                       [refactor-nrepl "2.4.0"]
                                       [leiningen-core "2.8.1"]]}}

  :test-paths ["integration" "test"]

  :aliases {"unit-tests" ["midje" "midje-nrepl.*"]
            "integration" ["midje" "integration.*"]})
