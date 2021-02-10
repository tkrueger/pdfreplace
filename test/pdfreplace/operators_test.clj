(ns pdfreplace.operators-test
  (:require  [pdfboxing.common :as common]
             [pdfreplace.operators :refer [cos-float cos-int 
                                           cos-name cos-op 
                                           cos-string
                                           parse-operators
                                           tokenize]]
             [pdfreplace.text :refer [with-text with-font-info 
                                       with-unicode
                                       with-replacement]]
             [pdfreplace.test-utils :refer [remove-equals]]
             [clojure.test :refer [deftest testing is]]
             [pdfreplace.pdf :refer [page fonts parse-tokens]]))

(deftest parsing-operators
  (testing "parses operators from tokens"
    (let [tokens [(cos-op "BT")
                  (cos-name "F1") (cos-int 24) (cos-op "Tf")
                  (cos-int 1) (cos-int 0) (cos-int 0) (cos-int 1) (cos-float 263.69) (cos-float 697.18) (cos-op "Tm")]
          ops (parse-operators tokens)]
      (is (= [{:operator (cos-op "BT") :args []}
              {:operator (cos-op "Tf") :args [(cos-name "F1") (cos-int 24)]}
              {:operator  (cos-op "Tm") :args [(cos-int 1) (cos-int 0) (cos-int 0) (cos-int 1) (cos-float 263.69) (cos-float 697.18)]}]
             (take 3 ops))))))

(defn lorem-ipsum-data
  []
  (let [file "test/lorem_ipsum.pdf"
        doc (common/obtain-document file)
        page1 (page doc 0)
        tokens (parse-tokens page1)
        operators (parse-operators tokens)
        fonts (fonts page1)]
    ;(.close doc)
    {:file file
     :doc doc
     :page1 page1
     :tokens tokens
     :operators operators
     :fonts fonts}))

(deftest tokenize-test
  (comment
    (testing "token roundtrip"
      (let [tokens [(cos-op "BT")
                    (cos-name "F1") (cos-int 24) (cos-op "Tf")
                    (cos-int 1) (cos-int 0) (cos-int 0) (cos-int 1) (cos-float 263.69) (cos-float 697.18) (cos-op "Tm")]
            ops (parse-operators tokens)
            tokenized (tokenize (fn [_ x] x) ops)]
        (is (= tokens
               tokenized)))))

  (testing "token roundtrip on file"
    (let [{:keys [fonts tokens]} (lorem-ipsum-data)
          ops (-> (parse-operators tokens)
                  (with-font-info)
                  (with-text)
                  (#(with-unicode fonts %))
                  (with-replacement #"Lorem" "Foo"))
          tokenized (tokenize (fn [_ x] x) ops)
          ]
      (is (= (count tokens) (count tokenized)))))

  (testing "uses encoder"
    (let [tokenized (tokenize (fn [_ x] "encoded")
                              [{:operator (cos-op "Tj")
                                :replaced "foo"
                                :args [(cos-string "foo")]}])]
      (is (= "encoded"
             (-> tokenized
                 first
                 .getString))))))

