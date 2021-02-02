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

  ; in this case, the array contains a letter or two, followed by an int
  ; defining the position (see page 331 of 
  ; https://www.adobe.com/content/dam/acom/en/devnet/pdf/pdfs/pdf_reference_archives/PDFReference.pdf)).
  ; To detect, we can extract the text per array, but replacing it would be
  ; either more complicated or ditch the positioning information. 
  (testing "has trouble with other doc formats"
    (replace-text "test/lorem_ipsum.pdf"
                  "target/lorem_ipsum_replaced.pdf"
                  {#"Lorem" "foo"})
    (let [text (pdftext/extract "target/lorem_ipsum_replaced.pdf")]
      ; obviously, we'd want to see foo in there, but for now, expected behavior
      ; is to fail in this...
      (is (not (string/includes? text "foo")))
      (is (string/includes? text "Lorem")))))
