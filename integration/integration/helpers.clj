(ns integration.helpers
  (:require [clojure.java.io :as io]
            [diehard.core :refer [with-retry]]
            [nrepl :as nrepl]))

(def ^:private octocat-dir "dev-resources/octocat")

(defn- get-nrepl-port []
  (with-retry {:retry-on        [java.io.FileNotFoundException java.lang.NumberFormatException]
               :max-duration-ms 60000}
    (-> (io/file octocat-dir ".nrepl-port")
        slurp
        Integer/parseInt)))

(defn send-message [message]
  (with-open [conn (nrepl/connect :port (get-nrepl-port))]
    (-> (nrepl/client conn 6000)
        (nrepl/message message)
        doall)))
