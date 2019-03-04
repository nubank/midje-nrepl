(ns midje-nrepl.reporter
  (:require [clojure.pprint :as pprint]
            [midje-nrepl.misc :as misc]
            [midje.config :as midje.config]
            [midje.data.fact :as fact]
            [midje.emission.plugins.default-failure-lines :as failure-lines]
            [midje.emission.plugins.silence :as silence]
            [midje.emission.state :as midje.state]
            [midje.util.exceptions :as midje.exceptions]))

(def ^:dynamic *report* nil)

(def no-tests {:results {}
               :summary {:check 0 :error 0 :fact 0 :fail 0 :ns 0 :pass 0 :to-do 0}})

(defn reset-report! [ns file]
  {:pre [(instance? clojure.lang.Namespace ns) (instance? java.io.File file)]}
  (reset! *report*
          (assoc no-tests                :testing-ns (symbol (str ns))
                 :file file)))

(defn summarize-test-results! []
  (let [namespace  (@*report* :testing-ns)
        results    (get-in @*report* [:results namespace])
        counters   (->> results (map :type) frequencies)
        namespaces (-> @*report* :results keys count)
        facts      (->> results (keep :id) distinct count)
        checks     (->> counters vals (apply +))]
    (swap! *report* update :summary
           merge (assoc counters  :check checks :fact facts :ns namespaces))))

(defn starting-to-check-top-level-fact [fact]
  (swap! *report* assoc :top-level-description [(fact/description fact)]))

(defn finishing-top-level-fact [_]
  (swap! *report* dissoc :top-level-description :current-test))

(defn- drop-possibly-retried-facts!
  "Some frameworks like nubank/selvage may alter Midje counters after
  checking a fact (e.g. due to a retry mechanism). This function
  provides a workaround for those cases by verifying whether counters
  were changed and dropping failures that may cause divergences in the
  final report."
  []
  (let [{current-failures :midje-failures}  (midje.state/output-counters)
        {previous-failures :midje-failures} (@*report* :output-counters)]
    (when (and previous-failures (not= current-failures previous-failures))
      (let [divergent-failures   (- previous-failures current-failures)
            {:keys [testing-ns]} @*report*]
        (swap! *report* update-in [:results testing-ns]
               (comp vec (partial drop-last divergent-failures)))))))

(defn- description-for [fact]
  (let [description (or (fact/best-description fact)
                        (pr-str (fact/source fact)))]
    (if-let [description-vec (@*report* :top-level-description)]
      (->> (conj description-vec description) distinct (remove nil?) vec)
      [description])))

(defn starting-to-check-fact [fact]
  (let [{:keys [testing-ns file]} @*report*]
    (swap! *report* assoc :current-test {:id         (fact/guid fact)
                                         :context    (description-for fact)
                                         :ns         testing-ns
                                         :file       file
                                         :line       (fact/line fact)
                                         :source     (pr-str (fact/source fact))
                                         :started-at (misc/now)})
    (drop-possibly-retried-facts!)))

(defn prettify-expected-and-actual-values [{:keys [expected actual] :as result-map}]
  (let [pretty-str #(with-out-str (pprint/pprint %))]
    (cond-> result-map
      expected (assoc :expected (pretty-str expected))
      actual   (assoc :actual (pretty-str actual)))))

(defn- conj-test-result! [{:keys [type] :as test-data}]
  (let [current-test (@*report* :current-test)
        ns           (@*report* :testing-ns)
        index        (count (get-in @*report* [:results ns]))
        test         (-> current-test
                         (assoc :index index)
                         (cond-> (not= type :to-do) (assoc :finished-at (misc/now)))
                         (merge test-data)
                         prettify-expected-and-actual-values)]
    (swap! *report* update-in [:results ns]
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
  (let [top-level-description (@*report* :top-level-description)
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

(defn info [message]
  (let [ns           (@*report* :testing-ns)
        last-result (dec (count (get-in @*report* [:results ns])))]
    (swap! *report* update-in [:results ns last-result :message]
           #(into (vec %) message))))

(defn future-fact [description-vec position]
  (conj-test-result! {:context description-vec
                      :line    (last position)
                      :type    :to-do}))

(defn finishing-fact [_]
  (swap! *report* assoc :output-counters (midje.state/output-counters)))

(def emission-map
  (merge silence/emission-map
         {:starting-to-check-top-level-fact starting-to-check-top-level-fact
          :starting-to-check-fact           starting-to-check-fact
          :pass                             pass
          :fail                             fail
          :info info
          :finishing-fact                   finishing-fact
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
  `(binding [*ns*                             ~ns
             *file*                           (str ~file)
             midje.config/*config*            (merge midje.config/*config* {:print-level :print-facts})
             midje.state/output-counters-atom (atom midje.state/fresh-output-counters)
             midje.state/emission-functions   emission-map
             *report*                         (atom {})]
     (try
       (reset-report! ~ns ~file)
       ~@forms
       (summarize-test-results!)
       (select-keys @*report* [:results :summary])
       (catch Exception err#
         (report-for-broken-ns ~ns ~file err#)))))
