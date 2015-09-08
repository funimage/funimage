(ns funimage.img.skeleton
  (:use [funimage img])
  (:require [clojure.string :as string]
            [clojure.java.io :as io])
  (:import [net.imglib2.algorithm.neighborhood Neighborhood RectangleShape]
           [net.imglib2.util Intervals]
           [net.imglib2.img ImagePlusAdapter Img]
           [net.imglib2.img.display.imagej ImageJFunctions]
           [net.imglib2.type NativeType]
           [net.imglib2.type.numeric NumericType ARGBType]
           [net.imglib2.type.numeric.real FloatType]
           [net.imglib2.view Views IntervalView]
           [net.imglib2 Cursor RandomAccess RandomAccessibleInterval Interval]
           [net.imagej.ops.thinning ThinningOp]
           [net.imglib2.algorithm.binary Thresholder]
           ))

(defn skeletonize
  "Skeletonize/thin an image."
  [img & args]
  (let [argmap (merge {:algorithm :morphological} (apply hash-map args) )
        ^net.imagej.ops.thinning.strategies.ThinningStrategy thinning-algo
        (cond (= (:algorithm argmap) :morphological) (net.imagej.ops.thinning.strategies.MorphologicalThinning. true)
              (= (:algorithm argmap) :guo-hall) (net.imagej.ops.thinning.strategies.GuoHallAlgorithm. true)
              (= (:algorithm argmap) :hilditch) (net.imagej.ops.thinning.strategies.HilditchAlgorithm. true)
              (= (:algorithm argmap) :zhang-suen) (net.imagej.ops.thinning.strategies.ZhangSuenAlgorithm. true))
        ^ThinningOp thinner (ThinningOp. thinning-algo
                              true
                              (.factory img))]
    (.compute thinner img img)))
  
