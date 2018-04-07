(defproject octocat "2.8.3"

  :description "FIXME: write description"

  :plugins [[lein-midje "3.2.1"]
            [midje-nrepl "0.1.0-SNAPSHOT"]]

  :middleware [midje-nrepl.leiningen-plugin/middleware]

  :dependencies [[org.clojure/clojure "1.9.0"]]

  :profiles {:dev {:dependencies [[midje "1.9.2-alpha3"]]}})
