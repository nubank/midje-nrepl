(ns midje-nrepl.project-info-test
  (:require [matcher-combinators.matchers :as m]
            [matcher-combinators.midje :refer [match]]
            [midje-nrepl.project-info :as project-info]
            [midje.sweet :refer :all]))

(def lein-project-with-source-paths '(defproject octocat "1.0.0"
                                       :dependencies [[org.clojure/clojure "1.9.0"]]
                                       :source-paths ["src/main/clojure" "src"]))

(def lein-project-with-no-source-paths '(defproject octocat "1.0.0"
                                          :dependencies [[org.clojure/clojure "1.9.0"]]))

(def lein-project-with-test-paths '(defproject octocat "1.0.0"
                                     :dependencies [[org.clojure/clojure "1.9.0"]]
                                     :test-paths ["test" "src/test/clojure"]))

(def lein-project-with-no-test-paths '(defproject octocat "1.0.0"
                                        :dependencies [[org.clojure/clojure "1.9.0"]]))

(facts "about getting information of the current project"

       (fact "returns a java.io.File representing the file where the namespace in question is declared"
             (.getPath (project-info/file-for-ns 'octocat.arithmetic-test))
             => #"test/octocat/arithmetic_test.clj$")

       (fact "reads the project.clj file of the current project and returns its contents as a list"
             (project-info/read-leiningen-project)
             => (match (m/prefix (list 'defproject 'nubank/midje-nrepl string?))))

       (fact "reads the project.clj file of the current project and returns it as a map"
             (project-info/read-project-map)
             => (match {:name         'octocat
                        :version      "1.0.0"
                        :dependencies [['org.clojure/clojure "1.9.0"]]
                        :test-paths   ["test" "src/test/clojure"]})
             (provided
              (project-info/read-leiningen-project) => lein-project-with-test-paths))

       (tabular (fact "returns true when the given directory exists in the project's working dire"
                      (project-info/existing-dir? ?directory) => ?result)
                ?directory ?result
                "test" true
                "integration" true
                "project.clj" false
                "non-existing-dir" false)

       (fact "returns the source paths of the project in question sorted alphabetically"
             (project-info/get-source-paths)
             => ["src" "src/main/clojure"]
             (provided
              (project-info/read-leiningen-project) => lein-project-with-source-paths
              (project-info/existing-dir? "src") => true
              (project-info/existing-dir? "src/main/clojure") => true))

       (fact "when the project doesn't declare source paths, assumes `src` by default"
             (project-info/get-source-paths)
             => ["src"]
             (provided
              (project-info/read-leiningen-project) => lein-project-with-no-source-paths
              (project-info/existing-dir? "src") => true))

       (fact "if the source path is declared in the project.clj, but doesn't exist in the current project, it isn't returned"
             (project-info/get-source-paths)
             => ["src"]
             (provided
              (project-info/read-leiningen-project) => lein-project-with-source-paths
              (project-info/existing-dir? "src") => true
              (project-info/existing-dir? "src/main/clojure") => false))

       (fact "returns the test paths of the project in question, sorted alphabetically"
             (project-info/get-test-paths)
             => ["src/test/clojure" "test"]
             (provided
              (project-info/read-leiningen-project) => lein-project-with-test-paths
              (project-info/existing-dir? "test") => true
              (project-info/existing-dir? "src/test/clojure") => true))

       (fact "when the project doesn't declare test paths, assumes `test` by default"
             (project-info/get-test-paths)
             => ["test"]
             (provided
              (project-info/read-leiningen-project) => lein-project-with-no-test-paths
              (project-info/existing-dir? "test") => true))

       (fact "if the test path is declared in the project.clj, but doesn't exist in the current project, it isn't returned"
             (project-info/get-test-paths)
             => ["test"]
             (provided
              (project-info/read-leiningen-project) => lein-project-with-test-paths
              (project-info/existing-dir? "test") => true
              (project-info/existing-dir? "src/test/clojure") => false))

       (fact "returns all namespaces declared within the supplied paths sorted alphabetically"
             (project-info/find-namespaces-in ["test/octocat"])
             => ['octocat.arithmetic-test
                 'octocat.colls-test
                 'octocat.mocks-test
                 'octocat.no-tests]))
