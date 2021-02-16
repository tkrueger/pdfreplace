(ns pdfreplace.operators
  (:require [clojure.string :as string]
            [pdfreplace.config :refer [min-font-size]])
  (:import [org.apache.pdfbox.cos COSString COSName COSInteger COSFloat COSArray]
           [org.apache.pdfbox.contentstream.operator Operator]))

(defn cos-op [name] (Operator/getOperator name))
(defn cos-string [text] (COSString. text))
(defn cos-int [i] (COSInteger/get (long i)))
(defn cos-name [n] (COSName/getPDFName n))
(defn cos-float [f] (COSFloat/get (str f)))
(defn into-cosarray [s]
  (doto (COSArray.)
    (.addAll s)))

(defn operator? [x] (instance? Operator x))
(defn not-operator? [x] (not (operator? x)))

(defn op-named?
  [token name]
  (and (operator? token)
       (= name (.getName token))))
(defn show-text-op? [token] (or (op-named? token "Tj")
                                (op-named? token "TJ")))
(defn select-font-op? [token] (op-named? token "Tf"))

(defmulti extract-text (fn [cosobj] (type cosobj)))
(defmethod extract-text COSString [s] (.getString s))
(defmethod extract-text COSArray
  [arr]
  (->> arr
       .toList
       (filter #(instance? COSString %))
       (map extract-text)
       string/join))
(defmethod extract-text :default [_] nil)

(defn parse-operators
  "Given a seq of tokens, parses them into [{:operator ... :args []}]"
  ([tokens] (parse-operators [] tokens))
  ([acc tokens]
   (if (empty? tokens)
     acc
     (if (operator? (first tokens))
       (parse-operators (conj acc {:operator (first tokens) :args []}) 
                        (rest tokens))
       (let [[args op] (split-with #(not (operator? %)) tokens)]
         (parse-operators (conj acc {:operator (first op) :args args}) 
                          (rest op)))))))

(defn tokenize
  [encoder ops]
  (reduce (fn [acc op]
            (if (and (show-text-op? (:operator op))
                     (contains? op :replaced))
              (let [font (:last-font-name op)
                    old-size (or (:last-font-size op) 0)
                    size (max (min-font-size) old-size)
                    [used-font encoded] (encoder [font (:replaced op)])
                    wrap? (not (= font used-font))]
                (if (instance? COSArray (first (:args op)))
                  (if (and wrap? size)
                    (concat acc [(cos-name used-font) (cos-int size) (cos-op "Tf")
                                 (into-cosarray [(COSString. encoded)]) (:operator op)
                                 (cos-name font) (cos-int old-size) (cos-op "Tf")])
                    (concat acc [(into-cosarray [(COSString. encoded)]) (:operator op)]))
                  (if (and wrap? size)
                    (concat acc [(cos-name used-font) (cos-int size) (cos-op "Tf")
                                 (COSString. encoded) (:operator op)
                                 (cos-name font) (cos-int old-size) (cos-op "Tf")])
                    (concat acc [(COSString. encoded) (:operator op)]))))
              (concat acc (flatten [(:args op) (:operator op)]))))
          []
          ops))

