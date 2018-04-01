(defproject midje-nrepl "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/tools.nrepl "0.2.13"]]

  :profiles {:dev {:dependencies [[midje "1.9.2-alpha3"]
                                  [nubank/matcher-combinators "0.2.4"]]}})
