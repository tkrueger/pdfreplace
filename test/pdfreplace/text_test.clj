(ns pdfreplace.text-test
  (:require [clojure.test :refer [deftest testing is]]
            [pdfreplace.text :refer [add-text]]
            [pdfreplace.operators :refer [into-cosarray]])
  (:import [org.apache.pdfbox.contentstream.operator Operator]
           [org.apache.pdfbox.cos COSString COSInteger]))

(deftest text-context
  (testing "extracts text from COSArray"
    (let [op {:operator (doto  (Operator/getOperator "TJ"))
              :args (into-cosarray [(COSInteger/get 1) (COSString. "F") (COSInteger/get 2) (COSString. "ritz")])}]
      (is (= "Fritz" (:text (add-text op)))))
    (let [op {:operator (doto  (Operator/getOperator "TJ"))
              :args [(COSString. "Fritz")]}]
      (is (= "Fritz" (:text (add-text op)))))))
