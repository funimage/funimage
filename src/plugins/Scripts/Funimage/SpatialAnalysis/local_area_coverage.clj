; @ImagePlus(label="Target image",description="Input image (will be binarized)") Funimage.SpatialAnalysis.local-area-coverage/segments
; @Integer(label="Radius",description="Coverage within a ball of this radius will be analyzed.",value=25) Funimage.SpatialAnalysis.local-area-coverage/density-radius
; @OUTPUT ImagePlus Funimage.SpatialAnalysis.local-area-coverage/density-imp
(ns Funimage.SpatialAnalysis.local-area-coverage
  (:use [funimage img imp project conversion utils]
        [funimage.segmentation utils imp]
        [funimage.imp calibration roi threshold calculator statistics])
  (:import [net.imglib2.algorithm.neighborhood Neighborhood]
           [ij.process ImageConverter FloatProcessor ByteProcessor]))
 
;Should check if density-radius is less than this
;(max (get-width segments) (get-height segments))
  
(ij.IJ/run segments "Make Binary" "")
  
(def rois (imp-to-rois segments ))
     
(def segment-maps (for [^ij.gui.Roi roi rois]
                    (assoc (get-image-statistics (set-roi segments roi) :center-of-mass true :centroid true :ellipse true :circularity true :area true)
                           :roi roi)))

(let [density-maps (rolling-ball-statistics segment-maps density-radius)
      key-fn :area]
  (def density-imp (color-code-rois segments density-maps key-fn))
  (.setMinAndMax (.getProcessor density-imp) 0 (* java.lang.Math/PI density-radius density-radius)))