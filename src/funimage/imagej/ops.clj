(ns funimage.imagej.ops
  (:require [funimage.imagej :as ij]
            [clojure.string :as string]))

(defn make-typehint
  "Make the typehint for a command input's type."
  [input-type]
  ;(.println (System/err) (str input-type " " (.toString input-type)))
  ;(.println (System/err) (str input-type " " (.toString input-type)))
  (if-not input-type
    ""
    (let [type-string (.toString input-type)
          parts (string/split type-string #" ")]
      (if (= 1 (count parts))
        ;(str "^" (first parts))
        ""
        (let [classname (second parts)]
          ;(.println (System/err) (string/join " " parts))
          (cond (= classname "[J") "^longs"
                (= classname "[I") "^ints"
                (= classname "[D") "^doubles"
                (.startsWith classname "[[") ""
                :else
                (str "^" classname)))))))

(defn guess-type
  "Guess the type of a command input's type."
  [input-type]
  ;(.println (System/err) (str input-type " " (.toString input-type)))
  ;(.println (System/err) (str input-type " " (.toString input-type)))
  (if-not input-type
    ""
    (let [type-string (.toString input-type)
          parts (string/split type-string #" ")]
      (if (= 1 (count parts))
        ;(str "^" (first parts))
        ""
        (let [classname (second parts)]
          ;(.println (System/err) (string/join " " parts))
          (cond (= classname "[J") "longs"
                (= classname "[I") "ints"
                (= classname "[D") "doubles"
                (.startsWith classname "[[") ""
                :else
                (str classname)))))))

(def infos-ignore #{"eval" "help" "identity" "info" "infos" "join" "loop" "map" "module" "op" "ops" "parent" "namespace" "run" "slice"})

(def valid-ops (into #{} (.ops (.op ij/ij))))

#_(def op-list
   (doall
     (for [[op-name op-infos] (group-by #(.getName %)
                                        (filter #(valid-ops (.getName %))
                                                (filter #(not (infos-ignore (.getName %)))
                                                        (.infos (.op ij/ij)))))
           op-info op-infos]
       (eval 
         (let [expr (with-out-str
                      (let [op-expression "(.op ij/ij)"]
                        (let [parts (string/split (.getName op-info) #"\.")
                              cinfo (.cInfo op-info)
                              required-inputs (filter #(.isRequired %) (seq (.inputs cinfo)))
                              optional-inputs (filter #(not (.isRequired %)) (seq (.inputs cinfo)))
                              arg-list (string/join
                                         " "
                                         (map #(str (make-typehint (.getType %)) " " (.getName %))
                                              required-inputs))
                              args-to-pass (string/join
                                             " "
                                             (map #(.getName %)
                                                  required-inputs))
                              op-name (str op-name (when (> (count op-infos) 1)
                                                     (let [classname (.getClassName (.cInfo op-info))
                                                           tail-part (last (string/split classname #"\."))
                                                           extension (string/replace tail-part "$" "-")
                                                                       #_(if (.contains tail-part "$")
                                                                          (last (string/split tail-part #"\$"))
                                                                          tail-part)]
                                                       (str "." extension))))]
                          (println "(defn" (string/replace op-name "." "-"))
                          (println "\t\"" (.getTitle cinfo) "\"")
                          (println "\t[" arg-list "]")
                          (println (str "\t(." (second parts) " (." (first parts) " " op-expression ") " args-to-pass "))")))))]
           ;(println expr)
           (read-string expr))))))

(def op-list
  (doall
    (for [[op-name op-infos] (group-by #(.getName %)
                                       (filter #(valid-ops (.getName %))
                                               (filter #(not (infos-ignore (.getName %)))
                                                       (.infos (.op ij/ij)))))
          op-info op-infos]
      (let [op-expression "(.op ij/ij)"
            parts (string/split (.getName op-info) #"\.")
            cinfo (.cInfo op-info)
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
                                required-inputs))
            op-name (str op-name (when (> (count op-infos) 1)
                                   (let [classname (.getClassName (.cInfo op-info))
                                         tail-part (last (string/split classname #"\."))
                                         extension (string/replace tail-part "$" "-")
                                                     #_(if (.contains tail-part "$")
                                                        (last (string/split tail-part #"\$"))
                                                        tail-part)]
                                     (str "." extension))))
            function-name (string/replace op-name "." "-")
            doc-string (.getTitle cinfo)
            expr (read-string
                   (with-out-str
                     (println "(defn" function-name)
                     (println "\t\"" doc-string "\"")
                     (println "\t[" arg-list "]")
                     (println (str "\t(." (second parts) " (." (first parts) " " op-expression ") " args-to-pass "))"))))]
        (eval expr)
        {:function-name function-name
         :input-types (map #(guess-type (.getType %))
                           required-inputs)
         :output-types (map #(guess-type (.getType %))
                            (seq (.outputs cinfo)))
         }))))
