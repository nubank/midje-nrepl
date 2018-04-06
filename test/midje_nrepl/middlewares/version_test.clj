(ns midje-nrepl.middlewares.version-test
  (:require [clojure.java.io :as io]
            [midje-nrepl.middlewares.version :as version]
            [midje.sweet :refer :all]))

(def project_clj "(defproject midje-nrepl \"1.0.0\"
  :description \"FIXME\"
  :url \"http://example.com/FIXME\"
  :license {:name \"Eclipse Public License\"}
  :dependencies [[org.clojure/clojure \"1.9.0\"]])")

(facts "about the version middleware"
       (against-background
        (io/resource "midje-nrepl/midje-nrepl/project.clj") =>
        (io/input-stream (.getBytes project_clj)))

       (fact "returns the current version of this project"
             (version/get-current-version)
             => "1.0.0")

       (tabular (fact "returns a map describing the midje-nrepl's current version"
                      (version/version-info ?version)
                      => (merge ?result
                                {:version-string ?version}))
                ?version           ?result
                "1.0.0"            {:major "1" :minor "0" :incremental "0"}
                "10.8.15"          {:major "10" :minor "8" :incremental "15"}
                "6.10.1-SNAPSHOT"  {:major "6" :minor "10" :incremental "1" :qualifier "SNAPSHOT"})

       (fact "sends the current version of this project to the nREPL client"
             (let [message {:transport ..transport..}]
               (version/handle-version message)
               => irrelevant
               (provided
                (transport/send ..transport.. {:status  #{:done}
                                               :version {:major          "1"
                                                         :minor          "0"
                                                         :incremental    "0"
                                                         :version-string "1.0.0"}})
                => irrelevant))))
