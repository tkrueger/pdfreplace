(ns pdfreplace.text-test
  (:require [clojure.test :refer [deftest testing is]]
            [pdfreplace.text :refer [add-text encoder can-encode?]]
            [pdfreplace.operators :refer [into-cosarray]])
  (:import [org.apache.pdfbox.contentstream.operator Operator]
           [org.apache.pdfbox.cos COSString COSInteger]
           [org.apache.pdfbox.pdmodel.font PDFontFactory]))

(deftest text-context
  (testing "extracts text from COSArray"
    (let [op {:operator (doto  (Operator/getOperator "TJ"))
              :args (into-cosarray [(COSInteger/get 1) (COSString. "F") (COSInteger/get 2) (COSString. "ritz")])}]
      (is (= "Fritz" (:text (add-text op)))))
    (let [op {:operator (doto  (Operator/getOperator "TJ"))
              :args [(COSString. "Fritz")]}]
      (is (= "Fritz" (:text (add-text op)))))))

(deftest encoder-test
  (testing "when font can encode, that is used"
    (let [encode (encoder {"F0" (PDFontFactory/createDefaultFont)})
          [used-font _] (encode ["F0" "text"])]
      (is (= "F0" used-font))))

  (testing "when font cannot encode, another is used"
    (with-redefs  [can-encode? (fn [[name _]] (= name "F0"))]
      (let [encode (encoder {"F0" (PDFontFactory/createDefaultFont)
                             "F1" (PDFontFactory/createDefaultFont)})
            [used-font _] (encode ["F1" "text"])]
        (is (= "F0" used-font))))))
