(defproject midje-nrepl "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/tools.nrepl "0.2.13"]
                 [cider/orchard "0.1.0-SNAPSHOT"]
                 [rewrite-clj "0.6.0"]]

  :profiles {:dev {:dependencies [[midje "1.9.2-alpha3"]
                                  [nubank/matcher-combinators "0.2.3"]]
                   :plugins [[lein-midje "3.2.1"]]}
             :provided {:dependencies [[cider/cider-nrepl "0.17.0-SNAPSHOT"]
                                       [leiningen-core "2.8.1"]]}}

  :test-paths ["integration" "test"]

  :aliases {"unit-tests" ["midje" "midje-nrepl.*"]
            "integration" ["midje" "integration.*"]})
