(ns integration.eval-test
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

(facts "about evaluating code without loading tests"

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

       (fact "facts are evaluated but not run"
             (send-message {:op   "eval"
                            :code "(fact 1 => 2)"
                            :ns   "octocat.arithmetic-test"})
             => (match (list (map-without-key :out)
                             {:status ["done"]})))

       (fact "files are evaluated but their facts aren't executed"
             (send-message {:op        "load-file"
                            :file      (read-file-content arithmetic-test)
                            :file-path arithmetic-test})
             => (match (list (map-without-key :out)
                             {:status ["done"]}))))
