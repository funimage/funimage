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

(def infos-ignore #{"eval" "help" "identity" "info" "infos" "join" "loop" "map" "module" "op" "ops" "parent" "namespace" "run" "slice"})

(def valid-ops (into #{} (.ops (.op ij/ij))))


(doall
  (for [[op-name op-infos] (group-by #(.getName %)
                                     (filter #(valid-ops (.getName %))
                                             (filter #(not (infos-ignore (.getName %)))
                                                     (.infos (.op ij/ij)))))
        op-info op-infos]
    (eval 
      (let [expr (with-out-str
                   (let [op-expression "(.op ij/ij)"]
                     ;(.println (System/err) (str "defn " (.getName op-info) op-info))
                     ;(.println (System/err) (str "defn " (.getName op-info) " " (.getClassName (.cInfo op-info))))
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
                                                        extension (if (.contains tail-part "$")
                                                                    (last (string/split tail-part #"\$"))
                                                                    tail-part)]
                                                    (str "." extension))))]
                       #_(.println (System/err) (string/join " " (map #(.getType %) (seq (.inputs cinfo)))))
                       (println "(defn" (string/replace op-name "." "-"))
                       (println "\t\"" (.getTitle cinfo) "\"")
                       (println "\t[" arg-list "]")
                       (println (str "\t(." (second parts) " (." (first parts) " " op-expression ") " args-to-pass "))")))))]
        ;(println expr)
        (read-string expr)))))


