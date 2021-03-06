(ns leiningen.kibit
  (:require [clojure.tools.namespace :as clj-ns]
            [clojure.java.io :as io]
            [jonase.kibit.core :as kibit]))

(defn kibit
  "Suggest idiomatic replacements for patterns of code."
  [project]
  (let [paths (or (:source-paths project) [(:source-path project)])
        source-files (mapcat #(-> % io/file clj-ns/find-clojure-sources-in-dir)
                             paths)]
    (doseq [source-file source-files]
      (printf "== %s ==\n"
              (or (second (clj-ns/read-file-ns-decl source-file)) source-file))
      (with-open [reader (io/reader source-file)]
        (doseq [{:keys [line expr alt]} (kibit/check-file reader)]
          (printf "[%s] Consider %s instead of %s\n" line alt expr)))
      (flush))))
