(ns inspect
  (:require [pdfboxing.common :as common]
            [clojure.string :as string])
  (:import [org.apache.pdfbox.cos COSArray COSString]
           [org.apache.pdfbox.pdfparser PDFStreamParser]
           [org.apache.pdfbox.contentstream.operator Operator]))

;;
;; Some exploratory code for ways to deal with text that is spread across
;; multiple operators.
;;

(defn parse-operators [page]
  (let [parser (doto (PDFStreamParser. page)
                 (.parse))]
    (.getTokens parser)))

(def doc (common/obtain-document "test/lorem_ipsum.pdf"))

(def ops-p1  (parse-operators (.getPage doc 0)))

(defn operator? [x] (instance? Operator x))
(defn not-operator? [x] (not (operator? x)) )

(defn op-and-args
  "scans through until it encounters and operator. Returs that operator
   with all arguments passed on the way as an array [op args]"
  [q]
  (let [[args op-and-rest] (split-with not-operator? q)]
    [(first op-and-rest) args]))

(defn ops-seq
  "Returns a lazy seq of operators and their arguments."
  ([q] (lazy-seq (cons (op-and-args q) (ops-seq (rest q))))))

; filter out all operators of type TJ (rendering text)
(def tjs  (filter (fn [[o _]] (= (.getName o) "TJ")) (ops-seq ops-p1)))

(defn extract-text
  "given a COSArray, returns the contained text, filtering out positional
   arguments."
  [arr]
  (->> arr
       .toList
       (filter #(instance? COSString %))
       (map #(.getString %))
       clojure.string/join))

; list text of all TJ operators.
; Note that text is wrapping across TJs freely...
; Would need to figure out which TJs have part of the regex etc. Tricky.
(map #(let [[_ args] %
            arg1 (first args)]
        (cond
          (instance? COSArray arg1) (extract-text arg1)
          :else args)) tjs)

(let [[_ args] (first tjs)]
     (when (and (= 1 (count args)) (instance? COSArray (first args)))
       (extract-text (first args))))
