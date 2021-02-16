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
  (testing "when font can encode, uses that"
    (let [encoder (fn [[font text]] [font "replaced text"])
          ops [{:operator (cos-op "Tj") :replaced "Some text" :last-font-name "F0" :last-font-size 1}]]
      (is (= [(cos-string "replaced text") (cos-op "Tj")]
             (tokenize encoder ops)))))

  (testing "when different font used for encoding, selects font"
    (let [encoder (fn [[font text]] ["F10" "replaced text"])
          ops [{:operator (cos-op "Tj") :replaced "Some text" :last-font-name "F0" :last-font-size 12}]
          tokenized (tokenize encoder ops)]
      (is (= [(cos-name "F10") (cos-int 12) (cos-op "Tf")
              (cos-string "replaced text") (cos-op "Tj")
              (cos-name "F0") (cos-int 12) (cos-op "Tf")]
             tokenized))))

  (testing "uses minimum font size"
    (let [encoder (fn [[font text]] ["F10" "replaced text"])
          ops [{:operator (cos-op "Tj") :replaced "Some text" :last-font-name "F0" :last-font-size 1}]
          tokenized (tokenize encoder ops)]
      (is (= [(cos-name "F10") (cos-int 7) (cos-op "Tf")
              (cos-string "replaced text") (cos-op "Tj")
              (cos-name "F0") (cos-int 1) (cos-op "Tf")]
             tokenized))))

  (testing "token roundtrip"
    (let [tokens [(cos-op "BT")
                  (cos-name "F1") (cos-int 24) (cos-op "Tf")
                  (cos-int 1) (cos-int 0) (cos-int 0) (cos-int 1) (cos-float 263.69) (cos-float 697.18) (cos-op "Tm")]
          ops (parse-operators tokens)
          tokenized (tokenize identity ops)]
      (is (= tokens
             tokenized))))

  (testing "token roundtrip on file"
    (let [{:keys [fonts tokens]} (lorem-ipsum-data)
          ops (-> (parse-operators tokens)
                  (with-font-info)
                  (with-text)
                  (#(with-unicode fonts %))
                  (with-replacement #"Lorem" "Foo"))
          tokenized (tokenize identity ops)]
      (is (= (count tokens) (count tokenized)))))

  (testing "uses encoder"
    (let [tokenized (tokenize (fn [[f x]] [f "encoded"])
                              [{:operator (cos-op "Tj")
                                :replaced "foo"
                                :args [(cos-string "foo")]
                                :last-font-name "F0"
                                :last-font-size 12}])]
      (is (= "encoded"
             (-> tokenized
                 first
                 .getString))))))

