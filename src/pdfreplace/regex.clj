(ns pdfreplace.regex
  (:require [pdfboxing.common :as common]
            [clojure.string :as string])
  (:import [org.apache.pdfbox.cos COSArray COSString COSName]
           [org.apache.pdfbox.pdmodel PDDocument]
           [org.apache.pdfbox.pdmodel.common PDStream]
           [org.apache.pdfbox.pdfparser PDFStreamParser]
           [org.apache.pdfbox.pdfwriter ContentStreamWriter]))

(defn read-pdf
  [loc]
  (common/obtain-document loc))

(defn parse-operators [page]
  (let [parser (doto (PDFStreamParser. page)
                 (.parse))]
    (.getTokens parser)))

(defn into-cosarray [s]
  (let [cos (COSArray.)]
    (map #(.add cos %) s)))

(defn replace-all
  "Given a string and a map of regexes to replacement strings, applies
   clojure.string/replace for each pair.
   Returns text with all replacements made."
  [text replacements]
  (reduce (fn [t [search replace]]
            (string/replace t search replace))
          text
          replacements))

(defn -replace-fn [token replacements]
  (comment (instance? COSArray token) (doto token (.clear)
                                            (.addAll  (into-cosarray (map #(-replace-fn % replacements) (.toList token))))))
  (cond
    (instance? COSString token) (COSString. (replace-all (.getString token) replacements))
    :else token))

(defn -replace-text
  [replacements]
  (fn [token]
    (-replace-fn token replacements)))

(defn process-page [doc page-nr proc-fn]
  (let [page (.getPage doc page-nr)
        page-ops (parse-operators page)
        page-ops (map proc-fn page-ops)
        stream (PDStream. doc)]
    (with-open [os (.createOutputStream stream COSName/FLATE_DECODE)]
      (let [writer (ContentStreamWriter. os)]
        (.writeTokens writer page-ops)
        (.setContents page stream)))
    doc))

(defn replace-text
  [source target replacements]
  (let [src-pdf (read-pdf source)
        target-pdf (PDDocument. (.getDocument src-pdf))]
    (doall
     (for [page-nr (range 0 (.getNumberOfPages target-pdf))]
       (process-page target-pdf
                     page-nr
                     (-replace-text replacements))))
    (.save target-pdf target)))