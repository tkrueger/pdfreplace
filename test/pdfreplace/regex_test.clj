(ns pdfreplace.regex-test
  (:require [clojure.string :as string]
            [clojure.test :refer [deftest testing is]]
            [pdfboxing.text :as pdftext]
            [pdfreplace.regex :refer [replace-text]] :reload-all))

(deftest replace-text-test
  (testing "replaces text matched by regex"
    (replace-text "test/sample-pdf.pdf"
                  "target/sample-pdf-replaced.pdf"
                  {#"Depotabrechnung" "Ersatztext"})
    (let [text (pdftext/extract "target/sample-pdf-replaced.pdf")]
      (is (not (string/includes? text "Depotabrechnung")))
      (is (string/includes? text "Ersatztext"))))

  (testing "has trouble with text spanning COSArrays"
    (replace-text "test/lorem_ipsum.pdf"
                  "target/lorem_ipsum_replaced.pdf"
                  {#"Lorem" "foo"})
    (let [text (pdftext/extract "target/lorem_ipsum_replaced.pdf")]
      (is (string/includes? text "foo") "replaces most occurrences")

      ; TODO this is NOT what is wanted, one Lorem spans COSArrays, and isn't found
      (is (string/includes? text "Lorem") "does not find text spanning multiple COSArrays")
      (is (= 2 (count (string/split text #"Lorem"))) "but finds all other occurences"))))
