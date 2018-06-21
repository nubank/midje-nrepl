(ns midje-nrepl.middleware.format
  (:require             [clojure.tools.nrepl.misc :refer [response-for]]
                        [clojure.tools.nrepl.transport :as transport]
                        [midje-nrepl.formatter :as formatter]))

(defn handle-format [{:keys [code transport] :as message}]
  (let [formatted-code (formatter/format-tabular code {:alignment       :right
                                                       :center-headers? false
                                                       :border-spacing  1
                                                       :indent-size     2})]
    (transport/send transport (response-for message :formatted-code formatted-code))
    (transport/send transport (response-for message :status :done))))
