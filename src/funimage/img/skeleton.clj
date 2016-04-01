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
           [net.imagej ImageJ]
           [net.imglib2.algorithm.binary Thresholder]
           [net.imglib2.type.logic BitType]
           [net.imagej.ops.special Computers]
           ))

(defonce ij (net.imagej.ImageJ.))

;(def filename "/Volumes/Amnes/SheltonSarah/dA01__m18_df_2015-08-17.tif")
(def filename "/Volumes/Amnes/Claudia/TuftAnalysis/OIR_ERG_VEcad_IsoB45_crop_001/Mask.tif" )

(def dataset (.open (.datasetIO (.scifio ij)) filename))

(.show (.ui ij) dataset)

(def top (ThinningOp.))

(def input (.getImg (.getImgPlus dataset)))
			
;(def bitOp (Computers/unary (.op ij) Ops.Convert.Bit (BitType.) (.firstElement input)))
			
;(def bit-input (.map (.op ij) input bitOp (BitType.)))		

;(.compute1 top bit-input (.img (.create (.op ij)) bit-input (BitType.) (.factory bit-input)))



#_(defn skeletonize
   "Skeletonize/thin an image. 
Algorithms: :morphological, :guo-hall, :hilditch, :zhang-suen"
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
  
