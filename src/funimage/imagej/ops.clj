(ns funimage.imagej.ops
  (:require [funimage.imagej :as ij]
            [clojure.string :as string]))

(defn make-typehint
  "Make the typehint for a command input's type."
  [input-type]
  (if-not input-type
    ""
    (let [type-string (.toString input-type)
          parts (string/split type-string #" ")]
      (if (= 1 (count parts))
        ""
        (let [classname (second parts)]
          (cond (= classname "[J") "^longs"
                (= classname "[I") "^ints"
                (= classname "[D") "^doubles"
                (.startsWith classname "[[") ""
                :else
                (str "^" classname)))))))

(defn guess-type
  "Guess the type of a command input's type."
  [input-type]
  
  (if-not input-type
    ""
    (let [type-string (.toString input-type)
          parts (string/split type-string #" ")]
      (if (= 1 (count parts))
        ""
        (let [classname (second parts)]
          (cond (= classname "[J") "longs"
                (= classname "[I") "ints"
                (= classname "[D") "doubles"
                (.startsWith classname "[[") ""
                :else
                (str classname)))))))

(def infos-ignore #{"eval" "help" "identity" "info" "infos" "join" "loop" "map" "module" "op" "ops" "parent" "namespace" "run" "slice"})

(def valid-ops (into #{} (.ops (.op ij/ij))))

(def ops-namespaces (atom []))

(def op-list
  (doall
    (for [[op-name op-infos] (group-by #(.getName %)
                                       (filter #(valid-ops (.getName %))
                                               (filter #(not (infos-ignore (.getName %)))
                                                       (.infos (.op ij/ij)))))]
      (let [op-info (first op-infos)
            op-expression "(.op ij/ij)"
            parts (string/split (.getName op-info) #"\.")
            cinfo (.cInfo op-info)
            op-namespace (symbol (string/join
                                   "."
                                   (concat [(ns-name *ns*)]
                                         (butlast (string/split op-name #"\.")))))
            function-name (last (string/split op-name #"\."))
            doc-string (.getTitle cinfo)

            fn-defs (doall
                      (for [op-info op-infos]
                        (let [cinfo (.cInfo op-info)                            
                              required-inputs (filter #(.isRequired %) (seq (.inputs cinfo)))
                              optional-inputs (filter #(not (.isRequired %)) (seq (.inputs cinfo)))
                              arg-list (string/join
                                         " "
                                         (map #(str (let [tpe (guess-type (.getType %)) ]
                                                      (if (empty? tpe)
                                                        ""
                                                        (str "^"tpe))) 
                                                    " " (.getName %))
                                              required-inputs))
                              args-to-pass (string/join
                                             " "
                                             (map #(.getName %)
                                                  required-inputs))]
                          [(count required-inputs)
                           (with-out-str
                             (println "([" arg-list "]")
                             (println (str "\t(." (second parts) " (." (first parts) " " op-expression ") " args-to-pass "))")))])))
            arity-map (apply hash-map (flatten fn-defs))
            expr (read-string
                   (str "(fn " (string/join " " (vals arity-map)) ")"))]        
        ; Test if the NS exists, if it doesn't then make it          
        (when-not (try (ns-name op-namespace) (catch Exception e nil))
          (create-ns op-namespace)
          (swap! ops-namespaces conj op-namespace))
        ; Make the function and load it into the respective namespace
        (intern (the-ns op-namespace)
                (symbol function-name)
                (eval expr))
        ; This doesnt need to be captured anymore
        {:function-name function-name
         :expression expr
         :namespace op-namespace
         }))))
