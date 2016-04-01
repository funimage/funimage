(ns funimage.utils  
  (:use [funimage imagej])
  (:require [clojure.string :as string]
            [clojure.pprint :as pprint])
  (:import [java.io File]
           [ij.gui NewImage Toolbar]
           [ij.process ImageProcessor ByteProcessor ImageStatistics]
           
           [java.util.concurrent Executors]
           [javax.swing JFrame JMenu JMenuBar JMenuItem]
           [java.awt Canvas Graphics]
           
           [net.imglib2.img ImagePlusAdapter Img]
           [net.imglib2.img.display.imagej ImageJFunctions]
           [net.imglib2.type NativeType]
           [net.imglib2.type.numeric NumericType]
           [net.imglib2.type.numeric.real FloatType]
           [net.imglib2.algorithm.gauss3 Gauss3]
           [net.imglib2.algorithm.dog DifferenceOfGaussian]
           [net.imglib2.view Views]
           [net.imglib2 Cursor]))

(defn construct-op-call
  "Construct an op-call from OpInfo."
  [ops op-info]
  (let [op-call (loop [packs (string/split (.getName op-info) #"\.")
                       call '(.op ij)]
                  (if (empty? packs)
                    (concat call 
                            (map #(symbol (.getName %)) (.inputs (.cInfo op-info)))); getIOType to check for input/output/both, to test for mutable, if it is a computer out comes first, technically mutable
                    ;; type hinting from .getType
                    (recur (rest packs)
                           `(~(symbol (str "." (first packs))) ~call))))]
    `(defn ~(symbol (str "ops-" (string/replace (.getName op-info) "." "-")))
       ; Construct a doc string... add type info
       [& {:keys ~(vec (map #(symbol (.getName %)) (.inputs (.cInfo op-info))))
           :or ~(apply hash-map (interleave (map #(symbol (.getName %)) (.inputs (.cInfo op-info)))
                                            (repeat nil)))}]
       ~op-call)))

;; Add into ops.*. namespaces

(defn regenerate-ops
  "Regenerate ImagejOps wrappers."
  []
  (println "You probably don't mean to do this, doing nothing.")
  (when false
    (spit "src/funimage/imagej_ops.clj"
          (string/replace
            (with-out-str 
              (println "(ns funimage.imagej-ops (:use [funimage imagej]))\n")
              (doseq [op (filter #(.getName %) (.infos (.op ij)))]
                (pprint/pprint (construct-op-call (.op ij) op))
                (println)))
            "clojure.core/defn" "defn"))))

;(pprint/pprint (construct-op-call (.op ij) (first (filter #(.getName %) (.infos (.op ij))))))

; This will load them all to
#_(doseq [op tmp]
   (eval (construct-op-call (.op ij) op)))
