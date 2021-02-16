(ns pdfreplace.config)

(def config (atom {:min-font-size 7}))

(defn set-config! [m] (reset! config m))

(defn min-font-size [] (:min-font-size @config))

(defn verbose? [] (:verbose @config))