(ns pdfreplace.text
  (:require [pdfreplace.operators :as ops]
            [clojure.string :as string])
  (:import [org.apache.pdfbox.cos COSString COSName]))

(defn font-name
  [tf-op]
  (when-let [name (first (filter #(instance? COSName %) (:args tf-op)))]
    (.getName name)))

(defn with-font-info
  [ops]
  (let [tf (atom nil)]
    (map (fn [o]
           (when (ops/select-font-op? (:operator o))
             (when-let [name (font-name o)]
               (reset! tf name)))
           (assoc o :last-font-name @tf))
         ops)))

(defn add-text
  [{:keys [args operator] :as op}]
  (if (not (ops/show-text-op? operator))
    op
    (let [texts (map ops/extract-text args)
          all-text (string/join texts)]
      (assoc op
             :text all-text
             :texts texts))))

(defn with-text
  [op]
  (map add-text op))

(defn encoder 
  "Returns an encoder function using the given font map. Returned function expects two
   arguments: the font name and the text to encode."
  [fonts]
  (fn [font txt] (.encode (get fonts font)
                          txt)))
(defn byte->ubyte [byte] (bit-and byte 0xFF))

(defn unicode
  "Takes a font and a byte array or COSString. Returns bytes as unicode, as mapped by font."
  [f bytes]
  (let [barr (cond
               (instance? COSString bytes) (.getBytes bytes)
               (bytes? bytes) bytes
               (string? bytes) (.getBytes bytes))] ; TODO this seems to give the wrong bytes
    (map #(.toUnicode f %) (map byte->ubyte  barr))))

(defn add-unicode
  [fonts {:keys [last-font-name text texts] :as op}]
  (let [used-font (get fonts last-font-name)]
    (cond-> op
      text (assoc :unicode (clojure.string/join (unicode used-font (COSString. text))))
      texts (assoc :unicodes (map #(clojure.string/join (unicode used-font (COSString. %))) texts)))))

(defn with-unicode [fonts op] (map (partial add-unicode fonts) op))

(defn with-replacement
  [operators regex replacement]
  (map (fn [op]
         (if-let [unicode (:unicode op)]
           (assoc op :replaced (clojure.string/replace unicode regex replacement))
           op))
       operators))

(defn replace-all
  "Given a string and a map of regexes to replacement strings, applies
   clojure.string/replace for each pair. Returns text with all replacements made."
  [text replacements]
  (reduce (fn [t [search replace]]
            (string/replace t search replace))
          text
          replacements))