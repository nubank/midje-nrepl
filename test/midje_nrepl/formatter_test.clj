(ns midje-nrepl.formatter-test
  (:require [matcher-combinators.core :as matcher-combinators]
            [midje-nrepl.formatter :as formatter]
            [midje.sweet :refer :all]))

(tabular (fact "determines the padding left and right to the supplied text according to the alignment option and the width"
           (formatter/paddings ?text ?alignment ?width)
           => {:padding-left  ?padding-left
               :padding-right ?padding-right})
  ?text ?alignment ?width ?padding-left ?padding-right
  "text" :center 4 0 0
  "text" :center 13 4 5
  "hello" :center 15 5 5
  "text" :center 20 8 8
  "hello" :center 20 7 8
  "text" :left 4 0 0
  "text" :left 20 0 16
  "text" :right 4 0 0
  "text" :right 20 16 0
  "hello" :left 11 0 6
  "hello" :right 15 10 0)

(def table ["?x" "?y" "?result"
            "4" "5" "9"
            "122" "3" "125"
            "1000" "250" "1250"])

(def right-aligned-table [{:leftmost-cell? true :padding-left 2 :padding-right 0} {:padding-left 1 :padding-right 0} {:rightmost-cell? true :padding-left 0 :padding-right 0}
                          {:leftmost-cell? true :padding-left 3 :padding-right 0} {:padding-left 2 :padding-right 0} {:rightmost-cell? true :padding-left 6 :padding-right 0}
                          {:leftmost-cell? true :padding-left 1 :padding-right 0} {:padding-left 2 :padding-right 0} {:rightmost-cell? true :padding-left 4 :padding-right 0}
                          {:leftmost-cell? true :padding-left 0 :padding-right 0} {:padding-left 0 :padding-right 0} {:rightmost-cell? true :padding-left 3 :padding-right 0}])

(def left-aligned-table [{:leftmost-cell? true :padding-left 0 :padding-right 2} {:padding-left 0 :padding-right 1} {:rightmost-cell? true :padding-left 0 :padding-right 0}
                         {:leftmost-cell? true :padding-left 0 :padding-right 3} {:padding-left 0 :padding-right 2} {:rightmost-cell? true :padding-left 0 :padding-right 6}
                         {:leftmost-cell? true :padding-left 0 :padding-right 1} {:padding-left 0 :padding-right 2} {:rightmost-cell? true :padding-left 0 :padding-right 4}
                         {:leftmost-cell? true :padding-left 0 :padding-right 0} {:padding-left 0 :padding-right 0} {:rightmost-cell? true :padding-left 0 :padding-right 3}])

(def centered-table [{:padding-right 1, :padding-left 1, :leftmost-cell? true} {:padding-right 1, :padding-left 0} {:padding-right 0, :padding-left 0, :rightmost-cell? true}
                     {:leftmost-cell? true :padding-left 1 :padding-right 2} {:padding-left 1 :padding-right 1} {:rightmost-cell? true :padding-left 3 :padding-right 3}
                     {:leftmost-cell? true :padding-left 0 :padding-right 1} {:padding-left 1 :padding-right 1} {:rightmost-cell? true :padding-left 2 :padding-right 2}
                     {:leftmost-cell? true :padding-left 0 :padding-right 0} {:padding-left 0 :padding-right 0} {:rightmost-cell? true :padding-left 1 :padding-right 2}])

(tabular (fact "determines the paddings for the supplied table according to the alignment options"
           (formatter/paddings-for-cells ?table {:alignment ?alignment}) => ?aligned-table)
  ?table ?alignment ?aligned-table
  table :right right-aligned-table
  table :left left-aligned-table
  table :center centered-table)

(def basic-tabular
  "(tabular (fact \"about basic arithmetic operations\"
  (?operation ?a ?b) => ?result)
  ?operation ?a ?b ?result
  + 2 5 10
  + 10 4 14
  - 100 25 75
    * 123 69 8487
  / 15 8 15/8
  / 4284 126 34)")

(def basic-tabular-w-deliniated-header
  "(tabular (fact \"about basic arithmetic operations\"
  (?operation ?a ?b) => ?result)
  [?operation ?a ?b ?result]
  + 2 5 10
  + 10 4 14
  - 100 25 75
    * 123 69 8487
  / 15 8 15/8
  / 4284 126 34)")

(def right-aligned-basic-tabular
  "(tabular (fact \"about basic arithmetic operations\"
  (?operation ?a ?b) => ?result)
  ?operation   ?a  ?b ?result
           +    2   5      10
           +   10   4      14
           -  100  25      75
           *  123  69    8487
           /   15   8    15/8
           / 4284 126      34)")

(def right-aligned-basic-tabular-w-deliniated-header
  "(tabular (fact \"about basic arithmetic operations\"
  (?operation ?a ?b) => ?result)
  [?operation   ?a  ?b ?result]
            +    2   5      10
            +   10   4      14
            -  100  25      75
            *  123  69    8487
            /   15   8    15/8
            / 4284 126      34)")

(def just-a-fact
  "(fact \"this isn't a tabular\"
  (+ 1 2) => 3)")

(def just-a-vector "[:blue :red :yellow]")

(def tabular-with-no-headers
  "(tabular (fact \"this is an invalid tabular\"
  (inc ?x) => ?y)
  1 2
  20 21
  100 101)")

(def malformed-tabular
  "(tabular (fact \"this one is malformed\"
  (inc ?x) => ?y)
  ?x ?y
  1 2
  10
  100 101)")

(defn throws-match [matcher]
  (throws clojure.lang.ExceptionInfo #(matcher-combinators/match? (matcher-combinators/match matcher (ex-data %)))))

(tabular (fact "formats the tabular fact according to default options"
           (formatter/format-tabular ?tabular)
           => ?result)
  ?tabular ?result
  basic-tabular right-aligned-basic-tabular
  basic-tabular-w-deliniated-header right-aligned-basic-tabular-w-deliniated-header
  just-a-fact (throws-match {:type ::formatter/no-tabular})
  just-a-vector (throws-match {:type ::formatter/no-tabular})
  tabular-with-no-headers (throws-match {:type ::formatter/no-table-headers})
  malformed-tabular (throws-match {:type ::formatter/malformed-table})
  )
