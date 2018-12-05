(defproject nubank/midje-nrepl "1.0.1"
  :description "nREPL middleware to interact with Midje"
  :url "https://github.com/nubank/midje-nrepl"
  :license {:name "Apache License, Version 2.0"
            :url  "http://www.apache.org/licenses/LICENSE-2.0"}
  :repositories  [["central"  {:url "https://repo1.maven.org/maven2/" :snapshots false}]
                  ["clojars" {:url   "https://clojars.org/repo/"
                              :creds :gpg}]]
  :deploy-repositories [["releases" :clojars]
                        ["snapshots" :clojars]]

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/tools.nrepl "0.2.13"]
                 [cider/orchard "0.3.0"]
                 [rewrite-clj "0.6.0"]]

  :profiles {:dev      {:dependencies [[diehard "0.7.2"]
                                       [midje "1.9.4"]
                                       [nubank/matcher-combinators "0.4.2"]]
                        :plugins      [[lein-midje "3.2.1"]]}
             :provided {:dependencies [[cider/cider-nrepl "0.17.0"]
                                       [refactor-nrepl "2.4.0"]
                                       [leiningen-core "2.8.1"]]}}

  :test-paths ["integration" "test"])
