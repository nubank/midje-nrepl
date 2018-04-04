(ns midje-nrepl.middlewares.version-test
  (:require [clojure.java.io :as io]
            [clojure.tools.nrepl.misc :refer [response-for]]
            [clojure.tools.nrepl.transport :as transport]
            [midje-nrepl.middlewares.version :as version]
            [midje.sweet :refer :all]))

(def project_clj "(defproject midje-nrepl \"x.x.x\"
  :description \"FIXME\"
  :url \"http://example.com/FIXME\"
  :license {:name \"Eclipse Public License\"}
  :dependencies [[org.clojure/clojure \"1.9.0\"]])")

(facts "about the version middleware"
       (against-background
        (io/resource "META-INF/leiningen/midje-nrepl/midje-nrepl/project.clj") =>
        (io/input-stream (.getBytes project_clj)))

       (fact "returns the current version of this project"
             (version/get-current-version)
             => "x.x.x")

       (fact "sends the current version of this project to the nREPL client"
             (let [message {:transport ..transport..}]
               (version/handle-version message)
               => irrelevant
               (provided
                (response-for message :status :done :midje-nrepl-version "x.x.x") => ..response..
                (transport/send ..transport.. ..response..) => irrelevant))))
