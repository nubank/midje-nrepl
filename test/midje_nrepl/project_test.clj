(ns midje-nrepl.project-test
  (:require [matcher-combinators.matchers :as m]
            [matcher-combinators.midje :refer [match]]
            [midje-nrepl.project :as project]
            [midje.sweet :refer :all]))

(def lein-project-with-test-paths '(defproject octocat "1.0.0"
                                     :dependencies [[org.clojure/clojure "1.9.0"]]
                                     :test-paths ["test" "src/test/clojure"]))

(def lein-project-with-no-test-paths '(defproject octocat "1.0.0"
                                        :dependencies [[org.clojure/clojure "1.9.0"]]))

(facts "about getting information of the current project"

       (fact "reads the project.clj file of the current project and returns its contents as a list"
             (project/read-leiningen-project)
             => (match (m/prefix (list 'defproject 'midje-nrepl string?))))

       (fact "reads the project.clj file of the current project and returns it as a map"
             (project/read-project-map)
             => (match {:project-name+version ['octocat "1.0.0"]
                        :dependencies         [['org.clojure/clojure "1.9.0"]]
                        :test-paths           ["test" "src/test/clojure"]})
             (provided
              (project/read-leiningen-project) => lein-project-with-test-paths))

       (tabular (fact "returns true when the given directory exists in the project's working dire"
                      (project/existing-dir? ?directory) => ?result)
                ?directory ?result
                "test" true
                "integration" true
                "project.clj" false
                "non-existing-dir" false)

       (fact "returns the test paths of the project in question, sorted alphabetically"
             (project/get-test-paths)
             => ["src/test/clojure" "test"]
             (provided
              (project/read-leiningen-project) => lein-project-with-test-paths
              (project/existing-dir? "test") => true
              (project/existing-dir? "src/test/clojure") => true))

       (fact "when the project doesn't declare test paths, assumes `test` by default"
             (project/get-test-paths)
             => ["test"]
             (provided
              (project/read-leiningen-project) => lein-project-with-no-test-paths
              (project/existing-dir? "test") => true))

       (fact "if the test path is declared in the project.clj, but doesn't exist in the current project, it isn't returned"
             (project/get-test-paths)
             => ["test"]
             (provided
              (project/read-leiningen-project) => lein-project-with-test-paths
              (project/existing-dir? "test") => true
              (project/existing-dir? "src/test/clojure") => false))

       (fact "returns all namespaces declared within the provided test paths sorted alphabetically"
             (project/get-test-namespaces-in ["test/octocat"])
             => ['octocat.arithmetic-test
                 'octocat.colls-test
                 'octocat.mocks-test
                 'octocat.no-tests]))
