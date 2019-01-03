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

(defn- check-facts [& {:keys [ns source line]}]
  {:pre [(symbol? ns)]}
  (let [the-ns (ensure-ns ns)
        file   (project-info/file-for-ns ns)
        reader (make-pushback-reader file source line)]
    (with-in-memory-reporter {:ns the-ns :file file}
      (clojure.main/repl
       :read                         #(read reader false %2)
       :need-prompt (constantly false)
       :prompt (fn [])
       :print  (fn [_])
       :caught #(throw %)))))

(defmacro saving-test-results!
  "Evaluates body (a set of forms that return a report map) and saves
  the test results in the current session."
  [& body]
  `(let [report# ~@body]
     (reset! test-results (report# :results))
     report#))

(defn run-test
  ([namespace source]
   (run-test namespace source 1))
  ([namespace source line]
   (saving-test-results!
    (check-facts :ns namespace :source source :line line))))

(defn run-tests-in-ns
  "Runs Midje tests in the given namespace.
   Returns the test report."
  [namespace]
  (saving-test-results!
   (check-facts :ns namespace)))

(defn- merge-test-reports [reports]
  (reduce (fn [a b]
            {:results (merge (:results a) (:results b))
             :summary (merge-with + (:summary a) (:summary b))})
          reporter/no-tests reports))

(defn run-all-tests
  "Runs all tests in the project or a subset of them, depending upon the supplied options.

  options is a PersistentMap with the valid keys:
  :inclusions - a regex to match namespaces against.
  :exclusions - a regex to match namespaces against. When both
  exclusions and inclusions are present, the former takes precedence
  over the later.
  :test-paths - a vector of test paths (strings) to restrict the test
  execution. Defaults to all known test paths declared in the
  project."
  [options]
  (let [{:keys [exclusions inclusions test-paths]
         :or   {test-paths (project-info/get-test-paths)}} options
        namespaces                                         (-> (project-info/find-namespaces-in test-paths)
                                                               (cond->>
                                                                   inclusions (filter #(re-find inclusions (name %)))
                                                                   exclusions (remove #(re-find exclusions (name %)))))]
    (saving-test-results!
     (->> namespaces
          (map #(check-facts :ns %))
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
  (saving-test-results!
   (->> @test-results
        (keep non-passing-tests)
        (map #(check-facts :ns (first %) :source (second %)))
        merge-test-reports)))
