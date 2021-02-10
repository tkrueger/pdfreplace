(ns pdfreplace.regex
  (:require [pdfreplace.pdf :refer [process-pdf replacer]]))

(defn replace-text
  [source target replacements]
  (process-pdf source
               target
               (replacer replacements)))

(comment
  (replace-text "trial.pdf"
                "delme.pdf"
                {#"Lorem" "foo"}))