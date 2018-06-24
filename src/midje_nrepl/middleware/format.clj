(ns midje-nrepl.middleware.format
  (:require             [clojure.tools.nrepl.misc :refer [response-for]]
                        [clojure.tools.nrepl.transport :as transport]
                        [midje-nrepl.formatter :as formatter]))

(defn- send-error [{:keys [transport] :as message} {:keys [type reason]}]
  (transport/send transport (response-for message :reason reason
                                          :status #{:error (keyword (name type))})))

(defn- formatter-exception? [type]
  (and type
       (= (symbol (namespace type))
          'midje-nrepl.formatter)))

(defn- handle-exception [exception message]
  (let [{:keys [type] :as ex-data} (ex-data exception)]
    (if (formatter-exception? type)
      (send-error message ex-data)
      (throw exception))))

(defn- try-to-format-tabular-fact [{:keys [code transport] :as message}]
  (let [formatted-code (formatter/format-tabular code)]
    (transport/send transport (response-for message :formatted-code formatted-code))
    (transport/send transport (response-for message :status :done))))

(defn handle-format [message]
  (try
    (try-to-format-tabular-fact message)
    (catch Exception e
      (handle-exception e))))
