(ns pdfreplace.main
  (:require [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]]
            [pdfreplace.regex :as textrep]))

(def cli-options
  [["-v" "--verbose"]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["Copies a pdf source file to a target, replacing each occurrance "
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
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}
      ;; custom validation on arguments
      (= 3 (count arguments))
      {:source (first arguments)
       :target (second arguments)
       :replacements (parse-replacements (nth arguments 2))
       :options options}
      :else ; failed custom validation => exit with usage summary
      {:exit-message (usage summary)})))

(defn exit [status msg]
  (println msg)
  (System/exit status))


(defn -main [& args]
  (let [{:keys [source target replacements options exit-message ok?]} (validate-args args)]
    (when exit-message
      (exit (if ok? 0 1) exit-message))
    (when (:verbose options)
      (println "copying" source "to" target "and applying replacements" replacements))
    (textrep/replace-text source target replacements)))