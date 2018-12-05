(ns midje-nrepl.reporter
  (:require [clojure.pprint :as pprint]
            [midje.config :as midje.config]
            [midje.data.fact :as fact]
            [midje.emission.plugins.default-failure-lines :as failure-lines]
            [midje.emission.plugins.silence :as silence]
            [midje.emission.state :as midje.state]
            [midje.util.exceptions :as midje.exceptions]))

(def report (atom {}))

(def no-tests {:results {}
               :summary {:check 0 :error 0 :fact 0 :fail 0 :ns 0 :pass 0 :to-do 0}})

(defn reset-report! [ns file]
  {:pre [(instance? clojure.lang.Namespace ns) (instance? java.io.File file)]}
  (reset! report
          (assoc no-tests                :testing-ns (symbol (str ns))
                 :file file)))

(defn summarize-test-results! []
  (let [namespace  (@report :testing-ns)
        results    (get-in @report [:results namespace])
        counters   (->> results (map :type) frequencies)
        namespaces (-> @report :results keys count)
        facts      (->> results (keep :id) distinct count)
        checks     (->> counters vals (apply +))]
    (swap! report update :summary
           merge (assoc counters  :check checks :fact facts :ns namespaces))))

(defn drop-irrelevant-keys! []
  (swap! report dissoc :testing-ns :file))

(defn starting-to-check-top-level-fact [fact]
  (swap! report assoc :top-level-description [(fact/description fact)]))

(defn finishing-top-level-fact [_]
  (swap! report dissoc :top-level-description :current-test))

(defn- description-for [fact]
  (let [description (or (fact/best-description fact)
                        (pr-str (fact/source fact)))]
    (if-let [description-vec (@report :top-level-description)]
      (->> (conj description-vec description) distinct (remove nil?) vec)
      [description])))

(defn starting-to-check-fact [fact]
  (let [{:keys [testing-ns file]} @report]
    (swap! report assoc :current-test {:id      (fact/guid fact)
                                       :context (description-for fact)
                                       :ns      testing-ns
                                       :file    file
                                       :line    (fact/line fact)
                                       :source  (pr-str (fact/source fact))})))

(defn prettify-expected-and-actual-values [{:keys [expected actual] :as result-map}]
  (let [pretty-str #(with-out-str (pprint/pprint %))]
    (cond-> result-map
      expected (assoc :expected (pretty-str expected))
      actual   (assoc :actual (pretty-str actual)))))

(defn- conj-test-result! [additional-data]
  (let [current-test (@report :current-test)
        ns           (@report :testing-ns)
        index        (count (get-in @report [:results ns]))
        test         (-> current-test
                         (assoc :index index)
                         (merge additional-data)
                         prettify-expected-and-actual-values)]
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

(defn- description-for-failing-tabular-fact [{:keys [description :midje/table-bindings] :or {description []}}]
  (let [top-level-description (@report :top-level-description)
        table-substitutions   (map (fn [[heading value]]
                                     (format "%s %s" (pr-str heading) (pr-str value))) table-bindings)]
    (as-> (into top-level-description description) description-vec
      (vec (distinct description-vec))
      (conj description-vec "With table substitutions:")
      (into description-vec table-substitutions)
      (remove nil? description-vec))))

(defn- failing-tabular-fact? [failure-map]
  (:midje/table-bindings failure-map))

(defn- conj-failure! [failure-map]
  (conj-test-result! (merge (when (failing-tabular-fact? failure-map)
                              {:context (description-for-failing-tabular-fact failure-map)})
                            (explain-failure failure-map)
                            {:line (-> failure-map :position last)
                             :type :fail})))

(defn- conj-error! [{:keys [expected-result-form actual position]}]
  (conj-test-result! {:line     (last position)
                      :expected expected-result-form
                      :error    (midje.exceptions/throwable actual)
                      :type     :error}))

(defn fail [failure-map]
  (if (midje.exceptions/captured-throwable? (:actual failure-map))
    (conj-error! failure-map)
    (conj-failure! failure-map)))

(defn future-fact [description-vec position]
  (conj-test-result! {:context description-vec
                      :line    (last position)
                      :type    :to-do}))

(def emission-map
  (merge silence/emission-map
         {:starting-to-check-top-level-fact starting-to-check-top-level-fact
          :starting-to-check-fact           starting-to-check-fact
          :pass                             pass
          :fail                             fail
          :finishing-top-level-fact         finishing-top-level-fact
          :future-fact                      future-fact}))

(defn- line-number-of-root-problem [exception]
  (let [line-number-re #"compiling:\(.*:(\d+):\d+\)"]
    (some->> (.getMessage exception)
             (re-find line-number-re)
             last
             Integer/parseInt)))

(defn report-for-broken-ns [ns file exception]
  (let [ns (symbol (str ns))]
    (-> no-tests
        (assoc-in [:results ns]
                  [{:context [(str ns ": namespace couldn't be loaded")]
                    :error   exception
                    :index   0
                    :ns      ns
                    :file    file
                    :line    (line-number-of-root-problem exception)
                    :type    :error}])
        (update :summary merge {:error 1 :ns 1}))))

(defmacro with-in-memory-reporter
  [{:keys [ns file]} & forms]
  `(binding [*ns*                           ~ns
             *file*                         (str ~file)
             midje.config/*config*          (merge midje.config/*config* {:print-level :print-facts})
             midje.state/emission-functions emission-map]
     (try
       (reset-report! ~ns ~file)
       ~@forms
       (summarize-test-results!)
       (drop-irrelevant-keys!)
       @report
       (catch Exception err#
         (report-for-broken-ns ~ns ~file err#)))))
