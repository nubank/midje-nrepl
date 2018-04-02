(ns midje-nrepl.reporter
  (:require [clojure.java.io :as io]
            [midje.config :as midje.config]
            [midje.data.fact :as fact]
            [midje.emission.plugins.default-failure-lines :as failure-lines]
            [midje.emission.plugins.silence :as silence]
            [midje.emission.state :as midje.state]
            [midje.util.exceptions :as midje.exceptions])
  (:import clojure.lang.Symbol))

(def report (atom nil))

(defn reset-report! [namespace]
  (reset! report
          {:results    {}
           :summary    {:error 0 :fail 0 :ns 0 :pass 0 :skip 0 :test 0}
           :testing-ns namespace}))

(defn summarize-test-results! []
  (let [namespace  (@report :testing-ns)
        results    (->> (get-in @report [:results namespace])
                        (group-by :type)
                        (map (fn [[type values]] {type (count values)}))
                        (into {}))
        namespaces (-> @report :results keys count)
        tests      (->> results vals (apply +))]
    (swap! report update :summary
           merge (assoc results :ns namespaces :test tests))))

(defn starting-to-check-top-level-fact [fact]
  (swap! report assoc :top-level-description [(fact/description fact)]))

(defn finishing-top-level-fact [_]
  (swap! report dissoc :top-level-description :current-test))

(defn- description-for [fact]
  (let [description (or (fact/best-description fact)
                        (pr-str (fact/source fact)))]
    (if-let [description-vec (@report :top-level-description)]
      (-> description-vec (conj description) distinct vec)
      [description])))

(defn starting-to-check-fact [fact]
  (let [line      (fact/line fact)
        namespace (fact/namespace fact)
        file      (-> namespace the-ns meta :file io/file)]
    (swap! report assoc :current-test {:context    (description-for fact)
                                       :ns         namespace
                                       :file       file
                                       :line       line
                                       :test-forms (pr-str (fact/source fact))})))

(defn- conj-test-result! [additional-data]
  (let [{:keys [context] :as current-test} (@report :current-test)
        ns                                 (@report :testing-ns)
        test                               (merge current-test additional-data)]
    (swap! report update-in [:results ns]
           (comp vec (partial conj)) test)))

(defn pass []
  (conj-test-result! {:type :pass}))

(defn- message-list-for [failure-map & {:keys [drop-n] :or {drop-n 0}}]
  (->> failure-map
       failure-lines/messy-lines
       flatten
       (drop drop-n)
       (keep identity)))

(defmulti explain-failure :type)

(defmethod explain-failure :actual-result-did-not-match-expected-value
  [{:keys [expected-result actual] :as failure-map}]
  {:expected expected-result
   :actual   actual
   :message  (message-list-for failure-map :drop-n 2)})

(defmethod explain-failure :actual-result-did-not-match-checker
  [{:keys [expected-result-form actual] :as failure-map}]
  {:expected expected-result-form
   :actual   actual
   :message  (message-list-for failure-map :drop-n 5)})

(defmethod explain-failure :default
  [failure-map]
  {:message (message-list-for failure-map)})

(defn- conj-error! [{:keys [expected-result-form actual position]}]
  (conj-test-result! {:line     (last position)
                      :expected expected-result-form
                      :error    (midje.exceptions/throwable actual)
                      :type     :error}))

(defn fail [failure-map]
  (if (midje.exceptions/captured-throwable? (:actual failure-map))
    (conj-error! failure-map)
    (conj-test-result! (merge (explain-failure failure-map)
                              {:line (-> failure-map :position last)
                               :type :fail}))))

(defn future-fact [description-vec position]
  (conj-test-result! {:context description-vec
                      :line    (last position)
                      :type    :skip}))

(def emission-map
  (merge silence/emission-map
         {:starting-to-check-top-level-fact starting-to-check-top-level-fact
          :starting-to-check-fact           starting-to-check-fact
          :pass                             pass
          :fail                             fail
          :finishing-top-level-fact         finishing-top-level-fact
          :future-fact                      future-fact}))

(defmacro with-reporter-for
  [^Symbol namespace & forms]
  `(binding [midje.config/*config*          (merge midje.config/*config* {:print-level :print-facts})
             midje.state/emission-functions emission-map]
     (reset-report! ~namespace)
     ~@forms
     (summarize-test-results!)
     @report))
