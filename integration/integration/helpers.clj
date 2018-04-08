(ns integration.helpers
  (:require [clojure.tools.nrepl :as nrepl]
            [clojure.java.io :as io]))

(def ^:private octocat-dir "dev-resources/octocat")

(defn- get-nrepl-port []
  (-> (io/file octocat-dir ".nrepl-port")
      slurp
      Integer/parseInt))

(defn send-message [message]
  (with-open [conn (nrepl/connect :port (get-nrepl-port))]
    (-> (nrepl/client conn 3000)
        (nrepl/message message)
        doall)))
