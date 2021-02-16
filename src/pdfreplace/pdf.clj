(ns pdfreplace.pdf
  (:require [pdfboxing.common :as common]
            [pdfreplace.operators :as ops]
            [pdfreplace.text :refer [with-font-info
                                     with-text
                                     encoder
                                     with-unicode
                                     replace-all
                                     encodable-fonts
                                     add-font]])
  (:import [org.apache.pdfbox.cos COSName]
           [org.apache.pdfbox.pdfparser PDFStreamParser]
           [org.apache.pdfbox.pdfwriter ContentStreamWriter]
           [org.apache.pdfbox.pdmodel PDDocument]
           [org.apache.pdfbox.pdmodel.common PDStream]))

(defn read-pdf
  [loc]
  (common/obtain-document loc))

(defn pages [doc]
  (.getPages doc))

(defn page-count [doc]
  (.getCount (pages doc)))

(defn page [doc n]
  (.getPage doc n))

(defn fonts
  "Given a page, returns all fonts used in it."
  [page]
  (let [resources (.getResources page)
        names (.getFontNames resources)]
    (reduce (fn [acc name] (assoc acc (.getName name) (.getFont resources name)))
            {}
            names)))

(defn parse-tokens
  "Given a page, retuns all tokens inside"
  [page]
  (let [parser (doto (PDFStreamParser. page)
                 (.parse))]
    (.getTokens parser)))

(defn rewrite-page
  [doc page-nr target tokens]
  (let [target-pdf (PDDocument. (.getDocument doc))
        stream (PDStream. target-pdf)
        target-p (.getPage target-pdf page-nr)]
    (with-open [os (.createOutputStream stream COSName/FLATE_DECODE)]
      (let [writer (ContentStreamWriter. os)]
        (.writeTokens writer tokens)
        (.setContents target-p stream)))
    (.save target-pdf target)
    (.close target-pdf)
    target-pdf))

(defn process-page
  [doc page proc-fn]
  (let [page-fonts (fonts page)
        page-fonts (if (empty? (encodable-fonts page-fonts))
                     (do
                       (println "adding an encodable font")
                       (add-font page)
                       (fonts page))
                     page-fonts)
        page-ops (-> (parse-tokens page)
                     (ops/parse-operators)
                     (with-font-info)
                     (with-text)
                     (#(with-unicode page-fonts %)))
        page-ops (map proc-fn page-ops)
        stream (PDStream. doc)
        tokenized (ops/tokenize (encoder page-fonts) page-ops)]
    (with-open [os (.createOutputStream stream COSName/FLATE_DECODE)]
      (let [writer (ContentStreamWriter. os)]
        (.writeTokens writer tokenized)
        (.setContents page stream)))
    doc))


(defn process-pdf
  [source target processor]
  (with-open [doc (read-pdf source)]
    (let [target-pdf (PDDocument. (.getDocument doc))]
      (doall ; to eagerly load all tokens before closing doc
       (map #(process-page doc % processor) (pages doc)))
      (.save target-pdf target))))

(defn replacer
  "Returns a processor function for use in process-pdf that will replace
   text according to a map of {regex replacement-text}."
  [replacements]
  (fn [{:keys [operator unicode] :as op}]
    (if (ops/show-text-op? operator)
      (do
        (tap> ["replacing text in" operator])
        (assoc op :replaced (replace-all unicode replacements)))
      op)))