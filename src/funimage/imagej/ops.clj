(ns funimage.imagej.ops
  (:require [funimage.imagej :as ij]
            [clojure.string :as string]))

(defn get-available-commands
  "Return a list of available commands."
  []
  (.getCommands (.command ij/ij)))

; -------- Temp

#_(doseq [ci (get-available-commands)]
   (println (.getIdentifier ci)))

#_(def create-ops (some #(when (.contains (.getIdentifier %) "CreateImgFromDimsAndType")
                          (println (.getIdentifier %))
                          %) (get-available-commands)))

#_(defn create-img-from-dims-and-type
   ""
   [dim tpe]
   (let [io-map {"in1" dim
                 "in2" tpe}
         module (.createModule create-ops)]
     (.setContext module (.context ij/ij))
     (.setInput module "in1" dim)
     (.setInput module "in2" tpe)
     (println "in1" dim)
     (println "in2" tpe)
     (.run module)
     #_(.run (.command ij/ij)
        create-ops
        true
        io-map)))

#_(create-img-from-dims-and-type (net.imglib2.FinalInterval. (long-array [10 10])) 
                                (net.imglib2.type.numeric.real.DoubleType.))

(require '[clojure.reflect :as r])

(def op-list (.infos (.op ij/ij)))

;(def this-op (nth op-list 637))
(def this-op (first op-list))

(def this-op (some #(when (= "create" (.getNamespace %)) %) (shuffle op-list))) this-op

(def this-ns (create-ns (symbol (string/join "." [(.getName *ns*) (.getNamespace this-op)]))))

;(def this-var (intern this-ns (symbol (.getName this-op)) ))

(.cInfo this-op)

(def this-module (.createModule (.cInfo this-op)))

;(.setInput this-module "in1" (net.imglib2.FinalInterval. (long-array [10 10])))
;(.setInput this-module "in2" (net.imglib2.type.numeric.real.DoubleType.))
;(.run this-module)

#_(net.imagej.ops.create.img.CreateImgFromDimsAndType. (net.imglib2.FinalInterval. (long-array [10 10])) 
                                                      (net.imglib2.type.numeric.real.DoubleType.))

(.inputs (.cInfo this-op))
 
#_(def tmp (net.imagej.ops.create.img.CreateImgFromDimsAndType.))

(def fi (net.imglib2.FinalInterval. (long-array [10 10])))
(def tpe (net.imglib2.type.numeric.real.DoubleType.))

;(.img (.create (.op ij/ij)) fi tpe)

(count (filter #(= (:name %) 'img) (:members (r/reflect (.create (.op ij/ij))))))

(def namespaces 
  (into #{}
        (filter identity
                (for [op-name (.ops (.op ij/ij))]
                  (first (string/split op-name #"\."))))))

;(.create (.op ij/ij))
(defmacro ops-ns-expression
  [ns-str]
  (let [ns-name (symbol (str "." ns-str))]    
    `(~ns-name (.op ij/ij))))

(def op-method (nth (seq (:members (r/reflect (ops-ns-expression "create")))) 17))

#_(defn op-expression
   [ns-str op-signature]
   (let [ns-name (symbol (str "." ns-str))
         op-str (symbol (str "." (:name op-signature)))]
     `(fn [^longs dimensions]
        (~op-str (~ns-name (.op ij/ij)) dimensions))))
#_(def ftmp (op-expression "create" op-method))

;(ftmp fi)


#_((op-expression "create" op-method) fi)

#_(let [op-expression "(.op ij/ij)"]
   (first (for [op-name (.ops (.op ij/ij))]    
            (let [parts (string/split op-name #"\.")]
              (println (str "(." (second parts) " (." (first parts) " " op-expression "))"))))))

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

; Makes all ops as independent functions
(def op-list
  (doall
    (for [op-info (filter #(valid-ops (.getName %))
                          (filter #(not (infos-ignore (.getName %)))
                                  (.infos (.op ij/ij))))]
      (eval 
        (let [expr (with-out-str
                     (let [op-expression "(.op ij/ij)"]
                       ;(.println (System/err) (str "defn " (.getName op-info) op-info))
                       (let [parts (string/split (.getName op-info) #"\.")
                             cinfo (.cInfo op-info)
                             arg-list (string/join
                                        " "
                                        (map #(str (make-typehint (.getType %)) " " (.getName %))
                                             (seq (.inputs cinfo))))
                             args-to-pass (string/join
                                        " "
                                        (map #(.getName %)
                                             (seq (.inputs cinfo))))]
                         #_(.println (System/err) (string/join " " (map #(.getType %) (seq (.inputs cinfo)))))
                         (println "(defn" (string/replace (.getName op-info) "." "-"))
                         (println "\t\"" (.getTitle cinfo) "\"")
                         (println "\t[" arg-list "]")
                         (println (str "\t(." (second parts) " (." (first parts) " " op-expression ") " args-to-pass "))")))))]
          ;(println expr)
          (read-string expr))))))

#_(def gops (group-by #(.getName %) (filter #(valid-ops (.getName %))
                                           (filter #(not (infos-ignore (.getName %)))
                                                   (.infos (.op ij/ij))))))

#_(for [[op-name op-infos] gops
       op-info op-infos]
   (str op-name (when (> (count op-infos) 1)
                  (let [classname (.getClassName (.cInfo op-info))
                        tail-part (last (string/split classname #"\."))
                        extension (if (.contains tail-part "$")
                                    (last (string/split tail-part #"\$"))
                                    tail-part)]
                    (str "." extension)))))

; Appends class name if there is a clash with the op name (because we can't distinguish between typed args)
(def op-list2
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
                             arg-list (string/join
                                        " "
                                        (map #(str (make-typehint (.getType %)) " " (.getName %))
                                             (seq (.inputs cinfo))))
                             args-to-pass (string/join
                                            " "
                                            (map #(.getName %)
                                                 (seq (.inputs cinfo))))
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
          (println expr)
          (read-string expr))))))

; Handle arguments better
(def op-list3
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
          (read-string expr))))))


