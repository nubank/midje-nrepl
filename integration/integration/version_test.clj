(ns integration.version-test
  (:require [integration.helpers :refer [send-message]]
            [matcher-combinators.midje :refer [match]]
            [midje.sweet :refer :all]))

(fact "gets the midje-nrepl's current version"
      (send-message {:op "version"})
      => (match (list {:midje-nrepl {:major       string?
                                     :minor       string?
                                     :incremental string?}})))
