(ns jonase.kibit.core
  "Kibit's core functionality uses core.logic to suggest idiomatic
   replacements for patterns of code and remove general code lint"
  (:require [clojure.core.logic :as logic]
            [clojure.java.io :as io]
            [jonase.kibit.rules :as core-rules])
  (:import [clojure.lang LineNumberingPushbackReader]))

;; The rule sets
;; -------------
;;
;; Rule sets are stored in individual files that have a top level
;; `(def rules '{...})`.  The collection of rules are in the `rules`
;; directory.
;;
;; For more information, see: [rule](#jonase.kibit.rules) namespace
(def all-rules core-rules/all-rules)


;; Parsing the lines/forms
;; -----------------------
;;
;; The unifier compares a form/line against a single rule.

;; The unification generates a map.
;;
;; The `:rule` is a vector of the matching rule/alt pair that was used
;; to produce the `:alt`, the ideal alternative.
;; The line number (`:line`) is extracted from the metadata of the line,
;; courtesy of LineNumberingPushbackReader (See `read-ns` and `check-file`)
(defn unify
  "TODO jonas"
  [expr rule]
  (let [[r s] (#'logic/prep rule)
        alt (first (logic/run* [alt]
                     (logic/== expr r)
                     (logic/== s alt)))]
    (when alt
      {:expr expr
       :rule rule
       :alt (try (if (string? alt) alt (seq alt)) (catch java.lang.IllegalArgumentException iae alt))
       :line (-> expr meta :line)})))

;; Loop over the rule set, recursively applying unification to find the best
;; possible alternative for the outermost form
(defn check-form
  "Given the outermost form of an expr/form, return a map containing the alternative suggestion info, or `nil`"
  ([expr]
     (check-form expr all-rules))
  ([expr rules]
     (when (sequential? expr)
       (loop [expr expr
              alt-map nil]
          (if-let [new-alt-map (some #(unify expr %) rules)]
            (recur (:alt new-alt-map)
                   new-alt-map)
            alt-map)))))

(declare expr-seq)
;; This walks across all the forms within a seq'd form/expression, checking each inner form
(defn check-expr
  "Given a full expression/form-of-forms/form, a map containing the alternative suggestion info, or `nil`"
  [expr]
  (clojure.walk/walk #(or (-> % check-form :alt) %) check-form expr))

;; Building the parsable forms
;; ---------------------------
;;
;; We treat each line as a single form, since logic will match any form sequence on the line
;; The line numbers are added to the lines/forms' to metadata, `^{:line}`

(defn read-ns
  "Generate a lazy sequence of lines from a [`LineNumberingPushbackReader`]( https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/LineNumberingPushbackReader.java )."
  [r]
  (lazy-seq
   (let [form (read r false ::eof)
         line-num (.getLineNumber r)]
     (when-not (= form ::eof)
       (cons (with-meta form {:line line-num}) (read-ns r))))))
 
(defn expr-seq
  "TODO jonas, just a quick one"
  [expr]
  (tree-seq sequential?
            seq
            expr))

(defn check-file
  "TODO jonas, just a quick one"
  ([reader]
     (check-file reader all-rules))
  ([reader rules]
     (keep check-expr
           (mapcat expr-seq (read-ns (LineNumberingPushbackReader. reader))))))

