(ns midje-nrepl.middlewares.version
  (:require [clojure.java.io :as io]
            [clojure.tools.nrepl.misc :refer [response-for]]
            [clojure.tools.nrepl.transport :as transport]))

(def ^:private project-clj "META-INF/leiningen/midje-nrepl/midje-nrepl/project.clj")

(defn get-current-version
  "Returns the current version of midje-nrepl."
  []
  (-> (io/resource project-clj)
      slurp
      read-string
      (nth 2)))

(defn handle-version
  "Handles the `midje-nrepl-version` op, by returning the current version of this project."
  [{:keys [transport] :as message}]
  (transport/send transport
                  (response-for message :status :done
                                :midje-nrepl-version (get-current-version))))
