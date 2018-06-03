(ns midje-nrepl.formatter-test
  (:require [midje-nrepl.formatter :as formatter]
            [midje.sweet :refer :all]))

(tabular (fact "centers the text according to the width"
               (formatter/center ?text ?width) => ?result)
         ?text ?width ?result
         "text" 4 "text"
         "text" 13 "    text     "
         "hello" 15 "     hello     "
         "text" 20 "        text        "
         "hello" 20 "       hello        ")

(tabular (fact "aligns the text according to the alignment option and the width"
               (formatter/align ?text ?alignment ?width) => ?result)
         ?text ?alignment ?width ?result
         "text" :center 4 "text"
         "text" :center 20 "        text        "
         "text" :left 4 "text"
         "text" :left 20  "text                "
         "text" :right 4 "text"
         "text" :right 20 "                text"
         "hello" :left 11 "hello      "
         "hello" :right 15 "          hello")

(def table ["?x" "?y" "?result"
            "4" "5" "9"
            "122" "3" "125"
            "1000" "250" "1250"])

(def right-aligned-table ["  ?x" " ?y" "?result"
                          "   4" "  5" "      9"
                          " 122" "  3" "    125"
                          "1000" "250" "   1250"])

(def right-aligned-table-with-centered-headers (into [" ?x " "?y " "?result"]
                                                     (drop 3 right-aligned-table)))

(def left-aligned-table ["?x  " "?y " "?result"
                         "4   " "5  " "9      "
                         "122 " "3  " "125    "
                         "1000" "250" "1250   "])

(def left-aligned-table-with-centered-headers (into [" ?x " "?y " "?result"]
                                                    (drop 3 left-aligned-table)))

(def centered-table [" ?x " "?y " "?result"
                     " 4  " " 5 " "   9   "
                     "122 " " 3 " "  125  "
                     "1000" "250" " 1250  "])

(tabular (fact "aligns the supplied table"
               (formatter/align-table ?table {:alignment       ?alignment
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
