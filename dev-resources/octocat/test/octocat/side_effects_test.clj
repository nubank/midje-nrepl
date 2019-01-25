(ns octocat.side-effects-test
  (:require [clojure.java.io :as io]
            [midje.sweet :refer :all]))

(def hello-world-file (io/file "target" "hello-world.txt"))

(defn safe-delete [file]
  (when (.exists file)
    (io/delete-file file)))

(defn write-hello-world []
  (spit hello-world-file "Hello world!"))

(facts "about write-hello-world" :mark1
       (against-background
        (before :contents (safe-delete hello-world-file)))

       (fact "writes a greeting file"
             (write-hello-world)
             (.exists hello-world-file) => true))
