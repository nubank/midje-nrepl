(defproject octocat "2.8.3"

  :description "FIXME: write description"

  :plugins [[lein-midje "3.2.1"]]

  :dependencies [[org.clojure/clojure "1.9.0"]]
  :profiles {:dev {:dependencies [[midje "1.9.4"]]}}

  :test-paths ["test" "integration"]

  :repl-options {:timeout 7000})
