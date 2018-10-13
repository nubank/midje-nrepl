(ns octocat.broken-namespace-test
  (:require  [midje.sweet :as midje]))

;; This is intentional for testing purposes. See integration.test-test.
(boom!)
