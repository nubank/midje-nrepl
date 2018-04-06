(ns integration-tests.test-utils
  (:require [clojure.tools.nrepl :as nrepl]
            [clojure.pprint :as pprint]
            [clojure.tools.nrepl.server :as nrepl.server]
            [midje-nrepl.nrepl :refer [nrepl-handler]]))

(defn- send-message [port message]
  (with-open [conn (nrepl/connect :port port)]
    (-> (nrepl/client conn 3000)
        (nrepl/message message)
        doall
        pprint/pprint)))

(defn start-nrepl-server []
  (let [{:keys [port] :as server} (nrepl.server/start-server :handler (nrepl-handler))]
    {:stop-nrepl-server (partial nrepl.server/stop-server server)
     :send-message      (partial send-message port)}))
