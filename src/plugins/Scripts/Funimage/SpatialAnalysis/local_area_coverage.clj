(ns plugins.Scripts.Funimage.SpatialAnalysis.local-area-coverage
  (:use [funimage img imp project conversion utils]
        [funimage.segmentation utils imp]
        [funimage.imp calibration roi threshold calculator statistics])
  (:require [clojure.string :as string])  
  (:import [net.imglib2.algorithm.neighborhood Neighborhood]
           [ij IJ ImagePlus WindowManager]
           [ij.measure ResultsTable]
           [ij.gui WaitForUserDialog PointRoi NewImage PolygonRoi Roi GenericDialog NonBlockingGenericDialog Line]
           [ij.plugin ImageCalculator Duplicator]
           [ij.plugin.frame RoiManager]
           [ij.process ImageConverter FloatProcessor ByteProcessor]
           [java.awt Button]
           [java.awt.event ActionListener]
           [java.io File]))
   
(def segments (ij.IJ/getImage))  
  
(ij.IJ/run segments "Make Binary" "")
  
(def rois (imp-to-rois segments #_:min-size #_100))
     
(def segment-maps (for [^ij.gui.Roi roi rois]
                    (assoc (get-image-statistics (set-roi segments roi) :center-of-mass true :centroid true :ellipse true :circularity true :area true)
                           :roi roi)))

(let [^ij.gui.GenericDialog gd (ij.gui.GenericDialog. "Local area coverage")]
  (.addSlider gd "Radius" 0 (max (get-width segments) (get-height segments)) 25)
  (.showDialog gd)
  (when-not (.wasCanceled gd)
    (def density-radius (.getNextNumber gd))

    ;(def density-radius 200)
    (def density-maps (rolling-ball-statistics segment-maps density-radius))
      
    (let [key-fn :area
          density-imp (color-code-rois segments density-maps key-fn)]
      (.setMinAndMax (.getProcessor density-imp) 0 (* java.lang.Math/PI density-radius density-radius))
      (show-imp density-imp))))  



