(ns midje-nrepl.test-runner
  (:require [clojure.java.io :as io]
            [clojure.main :as clojure.main]
            [midje-nrepl.project-info :as project-info]
            [midje-nrepl.reporter :as reporter :refer [with-in-memory-reporter]])
  (:import [clojure.lang LineNumberingPushbackReader Symbol]
           java.io.StringReader))

(def test-results (atom {}))

(defn get-exception-at [ns index]
  {:pre [(symbol? ns) (or (zero? index) (pos-int? index))]}
  (get-in @test-results [ns index :error]))

(defmacro caching-test-results [& forms]
  `(let [report# ~@forms]
     (reset! test-results (report# :results))
     report#))

(defn- source-pushback-reader [source line]
  (let [reader (LineNumberingPushbackReader. (StringReader. source))]
    (.setLineNumber reader line)
    reader))

(defn- make-pushback-reader [file source line]
  (if source
    (source-pushback-reader source line)
    (LineNumberingPushbackReader. (io/reader file))))

(defn- evaluate-facts [{:keys [ns source line] :or {line 1}}]
  {:pre [(symbol? ns) (or (nil? source) (string? source)) (pos-int? line)]}
  (let [file   (project-info/file-for ns)
        reader (make-pushback-reader file source line)]
    (with-in-memory-reporter {:ns ns :file file}
      (clojure.main/repl
       :read                         #(read reader false %2)
       :need-prompt (constantly false)
       :prompt (fn [])
       :print (fn [_])))))

(defn run-test [namespace line source]
  (caching-test-results
   (evaluate-facts {:ns namespace :source source :line line})))

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

(defn- non-passing-tests [results]
  (->> results
       (filter #(#{:error :fail} (:type %)))))

(defn re-run-failed-tests
  "Re-runs tests that have failed in the last execution.
  Returns the test report."
  []
  (->> @test-results
       non-passing-tests
       (map evaluate-facts)
       #_        merge-test-reports))

(defn run-all-tests []
  (let [test-paths (project-info/get-test-paths)]
    (caching-test-results (->> test-paths
                               project-info/get-test-namespaces-in
                               (map #(evaluate-facts {:ns %}))
                               merge-test-reports))))
(re-run-failed-tests)
