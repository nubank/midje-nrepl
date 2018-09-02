(ns integration.load-test
  (:require [integration.helpers :refer [send-message]]
            [matcher-combinators.midje :refer [match]]
            [midje.sweet :refer :all]
            [matcher-combinators.matchers :as m]
            [clojure.java.io :as io]
            [clojure.string :as string]))

(def octocat-tests "dev-resources/octocat/test")

(def arithmetic-tests "octocat/arithmetic_test.clj")

(defn read-arithmetic-test-file []
  (slurp (io/file octocat-tests arithmetic-tests)))

(facts "about evaluating code without loading tests"

       (fact "the nREPL continues to evaluate sexprs normally even with the wrap-load middleware in the stack"
             (send-message {:op "eval" :code "(* 7 8)"})
             => (match (m/in-any-order [{:value "56"}
                                        {:status ["done"]}])))

       (fact "the `load-file` op continues to work as expected too"
             (send-message {:op "load-file" :file (read-arithmetic-test-file) :file-path arithmetic-tests})
             => (match (m/embeds [{:out (complement string/blank?)}]))))

(send-message {:op "eval" :ns "octocat.arithmetic-test" :code "(println \"hello \" clojure.test/*load-tests*)
(with-out-str
(fact 1 => 2))"})
