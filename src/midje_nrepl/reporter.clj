(ns midje-nrepl.reporter
  (:require [clojure.java.io :as io]
            [midje.config :refer [*config*]]
            [midje.data.fact :as fact]
            [midje.emission.plugins.default-failure-lines :as failure-lines]
            [midje.emission.plugins.silence :as silence]
            [midje.emission.state :as state]))

(def report (atom nil))

(defn reset-report! [namespace]
  (reset! report
          {:results    {}
           :summary    {:error 0 :fail 0 :ns 1 :pass 0 :skip 0 :test 0}
           :testing-ns namespace}))

(defn summarize-results! []
  (let [namespace (@report :testing-ns)
        results   (->> (get-in @report [:results namespace])
                       (group-by :type)
                       (map (fn [[type values]] {type (count values)}))
                       (into {}))
        tests     (->> results vals (apply +))]
    (swap! report update :summary
           merge (assoc results :test tests))))

(defn starting-to-check-top-level-fact [fact]
  (swap! report assoc :top-level-description [(fact/description fact)]))

(defn finishing-top-level-fact [_]
  (swap! report dissoc :top-level-description :current-test))

(defn- description-for [fact]
  (let [description (fact/best-description fact)]
    (if-let [description-list (@report :top-level-description)]
      (-> description-list (conj description) distinct vec)
      [description])))

(defn starting-to-check-fact [fact]
  (let [line      (fact/line fact)
        namespace (fact/namespace fact)
        file      (-> namespace the-ns meta :file io/file)]
    (swap! report assoc :current-test {:context (description-for fact)
                                       :ns      namespace
                                       :file    file
                                       :line    line})))

(defn- index-for [namespace test-context]
  (->> (get-in @report [:results namespace])
       (take-while #(->> % :context (= test-context)))
       count))

(defn- conj-test-result! [additional-data]
  (let [{:keys [ns context] :as current-test} (@report :current-test)
        test                                  (-> current-test (merge additional-data) (assoc :index (index-for ns context)))]
    (swap! report update-in [:results ns]
           (comp vec (partial conj)) test)))

(defn pass []
  (conj-test-result! {:type :pass}))

(defn- diff-for [failure-map]
  (->> failure-map
       failure-lines/messy-lines
       (drop 2)
       (keep identity)))

(defn fail [failure-map]
  (let [{:keys [actual expected-result position]} failure-map
        [_ line]                                  position]
    (conj-test-result! {:line     line
                        :expected expected-result
                        :actual   actual
                        :diffs    (diff-for failure-map)
                        :type     :fail})))

(defn future-fact [description-list position]
  (conj-test-result! {:context description-list
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
  [namespace & forms]
  `(binding [*config*                 (merge *config* {:print-level :print-facts})
             state/emission-functions emission-map]
     (reset-report! ~namespace)
     ~@forms
     (summarize-results!)
     @report))
