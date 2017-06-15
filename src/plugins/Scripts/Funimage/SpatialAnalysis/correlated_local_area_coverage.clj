; @ImagePlus(label="Focal image",description="Measure centered at these segments (will be binarized)") focal-segments
; @ImagePlus(label="Correlation image",description="Take measurements on these segments (will be binarized)") correlation-segments
; @Integer(label="Radius",description="Coverage within a ball of this radius will be analyzed.",value=25) density-radius
; @OUTPUT ImagePlus density-imp

(use '[funimage imp project conversion]
     '[funimage.segmentation utils imp]
     '[funimage.imp calibration roi threshold calculator statistics])

(let [density-radius (min density-radius (max (get-width focal-segments) (get-height correlation-segments)))]
  (ij.IJ/run focal-segments "Make Binary" "")
  (ij.IJ/run correlation-segments "Make Binary" "")
  
  (let [focal-rois (imp-to-rois focal-segments)
        correlation-rois (imp-to-rois correlation-segments)   
        focal-segment-maps (for [^ij.gui.Roi roi focal-rois]
                             (assoc (get-image-statistics (set-roi focal-segments roi) :center-of-mass true :centroid true :ellipse true :circularity true :area true)
                                    :roi roi))
        correlation-segment-maps (for [^ij.gui.Roi roi correlation-rois]
                                   (assoc (get-image-statistics (set-roi correlation-segments roi) :center-of-mass true :centroid true :ellipse true :circularity true :area true)
                                          :roi roi))
        density-maps (correlated-rolling-ball-statistics focal-segment-maps correlation-segment-maps density-radius)
        key-fn :area]
    (def density-imp (show-imp (color-code-rois focal-segments density-maps key-fn)))
    (.setMinAndMax (.getProcessor density-imp) 0 (* java.lang.Math/PI density-radius density-radius))
    (update-imp density-imp)))