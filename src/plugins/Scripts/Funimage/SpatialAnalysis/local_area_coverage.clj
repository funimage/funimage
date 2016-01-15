; @ImagePlus(label="Target image",description="Input image (will be binarized)") segments
; @Integer(label="Radius",description="Coverage within a ball of this radius will be analyzed.",value=25) density-radius
; @OUTPUT ImagePlus Funimage.SpatialAnalysis.local-area-coverage/density-imp

(use '[funimage img imp project conversion]
     '[funimage.segmentation utils imp]
     '[funimage.imp calibration roi threshold calculator statistics])

(let [density-radius (min user/density-radius (max (get-width segments) (get-height segments)))]
  (ij.IJ/run segments "Make Binary" "")
  
  (let [rois (imp-to-rois segments )     
        segment-maps (for [^ij.gui.Roi roi rois]
                       (assoc (get-image-statistics (set-roi segments roi) :center-of-mass true :centroid true :ellipse true :circularity true :area true)
                              :roi roi))
        density-maps (rolling-ball-statistics segment-maps density-radius)
        key-fn :area]
    (def density-imp (show-imp (color-code-rois segments density-maps key-fn)))
    (.setMinAndMax (.getProcessor density-imp) 0 (* java.lang.Math/PI density-radius density-radius))
    (update-imp density-imp)))