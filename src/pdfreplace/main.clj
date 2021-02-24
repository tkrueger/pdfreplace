(ns pdfreplace.main
  (:require [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]]
            [pdfreplace.config :refer [set-config! verbose?]]
            [pdfreplace.regex :as textrep])
  (:gen-class))

(def cli-options
  [["-v" "--verbose"]
   ["-h" "--help"]
   ["-mfs" "--min-font-size SIZE" "Minimum font size. Defaults to 7."
    :parse-fn #(Integer/parseInt %) :default 7]])

(defn usage [options-summary]
  (->> ["Copies a pdf source file to a target, replacing each occurrence "
        "of any of the given regexes with their respective replacement strings."
        ""
        "Usage: pdfreplace [options] source target replacement-map"
        ""
        "  source       - source pdf file"
        "  target       - file to write the results to"
        "  replacements - either a parseable clojure map or a filename containing one."
        "                 Map is using regex (or strings) as keys (see clojure.string/replace)"
        "                 and strings as values."
        "Example: pdfreplace filea fileb '{#\"foo\" \"bar\"}'"
        "         Uses replacements as parseable Clojure code. Will replace all 'foo's with 'bar'."
        ""
        "Example: pdfreplace filea fileb replacements.edn"
        "         Uses replacements loaded from file 'replacements.edn'."
        ""
        "Options: "
        options-summary]
       (string/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn parse-replacements
  [arg]
  (let [parsed (read-string arg)]
    (cond
      (symbol? parsed) (read-string (slurp (str parsed)))
      (map? parsed) parsed
      :else (throw (RuntimeException. "Could not parse replacements")))))

(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with a error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (set-config! options)
    (cond (:help options) {:exit-message (usage summary) :ok? true}
          errors {:exit-message (error-msg errors)}
          (= 3 (count arguments)) {:source (first arguments)
                                   :target (second arguments)
                                   :replacements (parse-replacements (nth arguments 2))
                                   :options options}
          :else {:exit-message (usage summary)})))

(defn exit [status msg]
  (println msg)
  (System/exit status))


(defn -main [& args]
  (let [{:keys [source target replacements options exit-message ok?]} (validate-args args)]
    (when exit-message
      (exit (if ok? 0 1) exit-message))
    (when (verbose?)
      (println "copying" source "to" target "and applying replacements" replacements))
    (textrep/replace-text source target replacements)))