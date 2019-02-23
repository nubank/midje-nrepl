(ns midje-nrepl.middleware.inhibit-tests
  (:require [clojure.test :refer [*load-tests*]]
            [nrepl.transport :as transport :refer [Transport]]))

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
        (alter-var-root #'*load-tests* (constantly load-tests?)))
      (transport/send transport message))))

(defn- forward-with-transport-proxy [{:keys [transport load-tests?] :as message} base-handler]
  (let [current-value-of-*load-tests* *load-tests*]
    (alter-var-root #'*load-tests* (constantly load-tests?))
    (base-handler (assoc message :transport (transport-proxy transport current-value-of-*load-tests*)))))

(defn- evaluate-without-running-tests [{:keys [load-tests?] :as message} base-handler]
  (update message :session
          swap! assoc #'*load-tests* load-tests?)
  (base-handler message))

(defn handle-inhibit-tests [{:keys [op] :as message} base-handler]
  (let [message (update message :load-tests? (fnil #(Boolean/parseBoolean %) "false"))]
    (case op
      "eval"           (evaluate-without-running-tests message base-handler)
      ("refresh" "refresh-all" "warm-ast-cache") (forward-with-transport-proxy message base-handler))))
