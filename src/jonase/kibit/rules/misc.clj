(ns jonase.kibit.rules.misc)

(def rules
  '{(apply str (interpose ?x ?y)) (clojure.string/join ?x ?y)

    ;; mapcat
    (apply concat (apply map ?x ?y)) (mapcat ?x ?y)
    (apply concat (map ?x . ?y)) (mapcat ?x . ?y)

    ;; filter
    (filter (complement ?pred) ?coll) (remove ?pred ?coll) 
    (filter #(not (?pred ?x)) ?coll) (remove ?pred ?coll)})

