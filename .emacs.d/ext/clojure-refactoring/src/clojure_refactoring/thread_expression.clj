(ns clojure-refactoring.thread-expression
  (:use [clojure-refactoring.support core]
        [clojure.walk :only [postwalk]]
        clojure-refactoring.support.formatter)
  (:require [clojure-refactoring.support.parser :as parser])
  (:require [clojure-refactoring.ast :as ast]))

(def expression-threaders '#{->> -> clojure.core/->> clojure.core/->})

(def threaded?
     (all-of? seq? (comp expression-threaders first)))

(defn- expand-threaded [coll]
  (if (threaded? coll)
    (macroexpand-1 coll)
    coll))

(defn- expand-all-threaded [node]
  (postwalk expand-threaded node))

(defn- any-threaded? [node]
  (some #(tree-contains? node %) expression-threaders))

(defn thread-unthread [code]
  "Takes an expression starting with ->> or -> and unthreads it"
  (format-code
   (loop [node (read-string code)]
     (if (any-threaded? node)
       (recur
        (expand-all-threaded node))
       node))))

;;;;; Threading below here
(defn threading-fns-from-type [type]
  "Returns functions to be used by thread-with-type
based on what type of threading is going to be"
  ({'-> {:position-f (comp second ast/relevant-content)
         :all-but-position-f (comp but-second ast/relevant-content)}
    '->> {:position-f (comp last ast/relevant-content)
          :all-but-position-f (comp butlast ast/relevant-content)}} type))

(defn not-last-threading-node? [ast position-f]
  (and (ast/tag= :list (position-f ast))
       (ast/tag= :list (position-f (position-f ast)))))

(defn finish-threading [{content :content :as node}
                        new-ast thread-type]
  (let [{:keys [position-f all-but-position-f]}
        (threading-fns-from-type thread-type)
        useful-content (ast/relevant-content node)]
    (ast/conj
     new-ast
     (position-f node)
     (apply ast/list-without-whitespace (all-but-position-f node)))))

(defn thread-with-type [thread-type ast]
  (let [{:keys [position-f all-but-position-f]}
        (threading-fns-from-type thread-type)]
    (loop [node ast new-node ast/empty-list]
      (if (not-last-threading-node? node position-f)
        (recur (position-f  node)
               (ast/conj new-node (all-but-position-f node)))
        (finish-threading node new-node thread-type)))))

(defn thread-ast [thread-type ast]
  (apply ast/list-without-whitespace
         `(~(ast/symbol thread-type)
           ~@(->> (thread-with-type thread-type ast)
                  ast/relevant-content))))

(defn- construct-threaded [thread-type code]
  (->> (ast/strip-whitespace (parser/parse1 code))
       (thread-ast thread-type)
       format-ast
       ast/ast->string))

(defn thread-last [code]
     (construct-threaded '->> code))

(defn thread-first [code]
     (construct-threaded '-> code))
