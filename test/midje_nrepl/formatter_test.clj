(ns midje-nrepl.formatter-test
  (:require [midje-nrepl.formatter :as formatter]
            [midje.sweet :refer :all]))

(tabular (fact "determines the padding left and right to the supplied text according to the alignment option and the width"
               (formatter/padding ?text ?alignment ?width)
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

(def centered-headers [{:leftmost-cell? true :padding-left 1 :padding-right 1} {:padding-left 0 :padding-right 1} {:rightmost-cell? true :padding-left 0 :padding-right 0}])

(def right-aligned-table-with-centered-headers (into centered-headers
                                                     (drop 3 right-aligned-table)))

(def left-aligned-table [{:leftmost-cell? true :padding-left 0 :padding-right 2} {:padding-left 0 :padding-right 1} {:rightmost-cell? true :padding-left 0 :padding-right 0}
                         {:leftmost-cell? true :padding-left 0 :padding-right 3} {:padding-left 0 :padding-right 2} {:rightmost-cell? true :padding-left 0 :padding-right 6}
                         {:leftmost-cell? true :padding-left 0 :padding-right 1} {:padding-left 0 :padding-right 2} {:rightmost-cell? true :padding-left 0 :padding-right 4}
                         {:leftmost-cell? true :padding-left 0 :padding-right 0} {:padding-left 0 :padding-right 0} {:rightmost-cell? true :padding-left 0 :padding-right 3}])

(def left-aligned-table-with-centered-headers (into centered-headers
                                                    (drop 3 left-aligned-table)))

(def centered-table (into centered-headers
                          [{:leftmost-cell? true :padding-left 1 :padding-right 2} {:padding-left 1 :padding-right 1} {:rightmost-cell? true :padding-left 3 :padding-right 3}
                           {:leftmost-cell? true :padding-left 0 :padding-right 1} {:padding-left 1 :padding-right 1} {:rightmost-cell? true :padding-left 2 :padding-right 2}
                           {:leftmost-cell? true :padding-left 0 :padding-right 0} {:padding-left 0 :padding-right 0} {:rightmost-cell? true :padding-left 1 :padding-right 2}]))

(tabular (fact "determines the paddings for the supplied table according to the alignment options"
               (formatter/paddings-for-table ?table {:alignment       ?alignment
                                                     :center-headers? ?center-headers}) => ?aligned-table)
         ?table ?alignment ?center-headers ?aligned-table
         table :right false right-aligned-table
         table :left false left-aligned-table
         table :right true right-aligned-table-with-centered-headers
         table :left true left-aligned-table-with-centered-headers
         table :center true centered-table
         table :center false centered-table)

(def tabular1 "(tabular (fact \"about basic arithmetic operations\"
  (?operation ?a ?b) => ?result)
  ?operation ?a ?b ?result
  + 2 5 10
  + 10 4 14
  - 100 25 75
    * 123 69 8487
  / 15 8 15/8
  / 4284 126 34)")

(def formatted-tabular1 "(tabular (fact \"about basic arithmetic operations\"
  (?operation ?a ?b) => ?result)
  ?operation   ?a  ?b ?result
           +    2   5      10
           +   10   4      14
           -  100  25      75
           *  123  69    8487
           /   15   8    15/8
           / 4284 126      34)")

(fact "formats the tabular fact according to the supplied options"
      (formatter/format-tabular tabular1 {:alignment       :right
                                          :center-headers? false
                                          :border-spacing  1
                                          :indent-size     2})
      => formatted-tabular1)
