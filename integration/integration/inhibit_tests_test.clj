(ns integration.inhibit-tests-test
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [integration.helpers :refer [send-message]]
            [matcher-combinators.matchers :as m]
            [matcher-combinators.midje :refer [match]]
            [midje.sweet :refer :all]))

(def octocat "dev-resources/octocat")

(def core "src/octocat/core.clj")

(def arithmetic-test "test/octocat/arithmetic_test.clj")

(defn read-file-content [file-path]
  (slurp (io/file octocat file-path)))

(defn map-without-key [key]
  (fn [map]
    (not (set/subset? #{key} (set (keys map))))))

(facts "about evaluating code without running tests"

       (fact "the nREPL continues to evaluate sexprs normally even with the wrap-eval middleware in the stack"
             (send-message {:op "eval" :code "(* 7 8)"})
             => (match (m/in-any-order [{:value "56"}
                                        {:status ["done"]}])))

       (fact "the `load-file` op continues to work as expected too"
             (send-message {:op        "load-file"
                            :file      (read-file-content core)
                            :file-path core})
             => (match (list {:value "56"}
                             {:status ["done"]})))

       (fact "facts are evaluated but aren't run"
             (send-message {:op   "eval"
                            :code "(ns octocat.arithmetic-test
(:require [midje.sweet :refer :all]))"})
             =>             (match (m/embeds [{:status ["done"]}]))
             (send-message {:op   "eval"
                            :code "(fact 1 => 2)"
                            :ns   "octocat.arithmetic-test"})
             => (match (m/embeds [{:value #"(nil)|(#'octocat.arithmetic-test.*)"}
                                  {:status ["done"]}])))

       (fact "files are evaluated but their facts aren't executed"
             (send-message {:op        "load-file"
                            :file      (read-file-content arithmetic-test)
                            :file-path arithmetic-test})
             => (match (list (map-without-key :out)
                             {:status ["done"]})))

       (fact "facts are run when the client sends the parameter `load-tests?` set to true"
             (send-message {:op          "eval"
                            :load-tests? "true"
                            :code        "(fact 1 => 2)"
                            :ns          "octocat.arithmetic-test"})
             => (match (m/prefix [{:out (partial re-find #"FAIL")}]))))

(def hello-world-file (io/file octocat "target" "hello-world.txt"))

(defn safe-delete-hello-world-file []
  (when (.exists hello-world-file)
    (io/delete-file hello-world-file)))

(facts "about loading namespaces without running tests"

       (fact "when Midje facts in the namespace `octocat.side-effects-test` are run, a file called hello-world.txt is created"
             (safe-delete-hello-world-file)
             (send-message {:op          "eval"
                            :load-tests? "true"
                            :code        "(require 'octocat.side-effects-test :reload)"})
             (.exists hello-world-file) => true)

       (fact "the warm-ast-cache middleware continues working as expected"
             (safe-delete-hello-world-file)
             (send-message {:op "warm-ast-cache"})
             => (match [{:ast-statuses "(octocat.arithmetic-test \"OK\" octocat.side-effects-test \"OK\")"
                         :status       ["done"]}]))

       (fact "Midje facts weren't run by the warm-ast-cache middleware, so the file hello-world.txt wasn't created again"
             (.exists hello-world-file)
             => false)

       (facts "the Cider's wrap-refresh middleware continues working as expected"
              (send-message {:op "refresh"})
              => (match (m/embeds [{:status ["ok"]}
                                   {:status ["done"]}])))

       (facts "the operation `refresh-all` continues working too"
              (send-message {:op "refresh-all"})
              => (match (m/embeds [{:status ["ok"]}
                                   {:status ["done"]}])))

       (fact "Midje facts weren't run by the Cider's wrap-refresh middleware, so the file hello-world.txt wasn't created"
             (.exists hello-world-file)
             => false))
