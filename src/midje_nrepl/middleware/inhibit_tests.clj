(ns midje-nrepl.middleware.inhibit-tests
  (:require [clojure.tools.nrepl.transport :as transport :refer [Transport]]
            [midje.config :as midje.config]))

(defn- done? [{:keys [status]}]
  (contains? status :done))

(defn- transport-proxy [transport load-tests?]
  (reify Transport
    (recv [_]
      (transport/recv transport))
    (recv [_ timeout]
      (transport/recv transport timeout))

    (send [_ message]
      (when (done? message)
        (alter-var-root #'midje.config/*config* assoc :check-after-creation load-tests?))
      (transport/send transport message))))

(defn- forward-with-transport-proxy [{:keys [transport load-tests?] :as message} base-handler]
  (let [current-value-of-check-after-creation (midje.config/*config* :check-after-creation)]
    (alter-var-root #'midje.config/*config* assoc :check-after-creation load-tests?)
    (base-handler (assoc message :transport (transport-proxy transport current-value-of-check-after-creation)))))

(defn- evaluate-without-running-tests [{:keys [load-tests?] :as message} base-handler]
  (let [midje-config (assoc midje.config/*config* :check-after-creation load-tests?)]
    (update message :session
            swap! assoc #'midje.config/*config* midje-config)
    (base-handler message)))

(defn handle-inhibit-tests [{:keys [op] :as message} base-handler]
  (let [message (update message :load-tests? (fnil #(Boolean/parseBoolean %) "false"))]
    (case op
      "eval"                                     (evaluate-without-running-tests message base-handler)
      ("refresh" "refresh-all" "warm-ast-cache") (forward-with-transport-proxy message base-handler))))
