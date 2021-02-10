(ns pdfreplace.test-utils)

(defn remove-equals
  [a b]
  (drop-while #(= (first %) (second %))
              (partition 2 (interleave a b))))
