(ns midje-nrepl.test-runner
  (:require [clojure.java.io :as io]
            [clojure.main :as clojure.main]
            [clojure.string :as string]
            [midje-nrepl.project-info :as project-info]
            [midje-nrepl.reporter :as reporter :refer [with-in-memory-reporter]])
  (:import clojure.lang.LineNumberingPushbackReader
           java.io.StringReader))

(def test-results (atom {}))

(defn get-exception-at [ns index]
  {:pre [(symbol? ns) (or (zero? index) (pos-int? index))]}
  (get-in @test-results [ns index :error]))

(defn- source-pushback-reader [source line]
  {:pre [(string? source) (or (nil? line) (pos-int? line))]}
  (let [reader (LineNumberingPushbackReader. (StringReader. source))]
    (.setLineNumber reader (or line 1))
    reader))

(defn- make-pushback-reader [file source line]
  (if source
    (source-pushback-reader source line)
    (LineNumberingPushbackReader. (io/reader file))))

(defn- ensure-ns [namespace]
  (or (find-ns namespace)
      (binding [*ns* (the-ns 'user)]
        (eval `(ns ~namespace))
        (the-ns namespace))))

(defn- evaluate-facts [{:keys [ns source line]}]
  {:pre [(symbol? ns)]}
  (let [the-ns (ensure-ns ns)
        file   (project-info/file-for-ns ns)
        reader (make-pushback-reader file source line)]
    (with-in-memory-reporter {:ns the-ns :file file}
      (clojure.main/repl
       :read                         #(read reader false %2)
       :need-prompt (constantly false)
       :prompt (fn [])
       :print  (fn [_])))))

(defmacro caching-test-results [& forms]
  `(let [report# ~@forms]
     (reset! test-results (report# :results))
     report#))

(defn run-test
  ([namespace source]
   (run-test namespace source 1))
  ([namespace source line]
   (caching-test-results
    (evaluate-facts {:ns namespace :source source :line line}))))

(defn run-tests-in-ns
  "Runs Midje tests in the given namespace.
   Returns the test report."
  [namespace]
  (caching-test-results
   (evaluate-facts {:ns namespace})))

(defn- merge-test-reports [reports]
  (reduce (fn [a b]
            {:results (merge (:results a) (:results b))
             :summary (merge-with + (:summary a) (:summary b))})
          reporter/no-tests reports))

(defn run-all-tests []
  (let [test-paths (project-info/get-test-paths)]
    (caching-test-results
     (->> test-paths
          project-info/get-test-namespaces-in
          (map #(evaluate-facts {:ns %}))
          merge-test-reports))))

(defn- non-passing-tests [[namespace results]]
  (let [non-passing-items (filter #(#{:error :fail} (:type %)) results)]
    (when (seq non-passing-items)
      (->> (map :source non-passing-items)
           (string/join (System/lineSeparator))
           (format "(do %s)")
           (list namespace)))))

(defn re-run-non-passing-tests
  "Re-runs tests that didn't pass in the last execution.
  Returns the test report."
  []
  (caching-test-results
   (->> @test-results
        (keep non-passing-tests)
        (map #(evaluate-facts {:ns (first %) :source (second %)}))
        merge-test-reports)))
