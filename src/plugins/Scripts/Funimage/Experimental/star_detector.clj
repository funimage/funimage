; @ImagePlus(label="Target image",description="Input image (will be binarized)") imp
; @OUTPUT ImagePlus imp

(require '[funimage.imp :as ij1]
         '[funimage.segmentation.imp :as ij1seg]
         '[funimage.imp.statistics :as ij1stats])

(when-not (ij.IJ/getInstance)
  (def filename "/Users/kharrington/Data/Harrington_Kyle/SanchezLab/ShapesOnPetriDish.tif")
  (def imp (ij1/open-imp filename)))

(ij.IJ/run imp "8-bit" "")

(ij1/show-imp imp)

(defn clear-outside-plate
  "Clear the outside of a ROI."
  [imp roi]
  (ij1/set-roi imp roi)
  (ij.IJ/setBackgroundColor 0 0 0)
  (ij.IJ/run imp "Clear Outside" "")
  (ij1/set-roi imp nil))

(if (ij.IJ/getInstance)
  ; Get a ROI from UI if we're in IJ
  (let [dialog (ij.gui.NonBlockingGenericDialog. "Select a ROI")]
    (ij.IJ/setTool "oval")
    (.addMessage dialog "Select a ROI")
    (.showDialog dialog)

    ; Manual blocking for the dialog to complete
    (loop []
      (Thread/sleep 100)
      (when-not (or (.wasCanceled dialog)
                    (.wasOKed dialog)
                    (.windowClosed dialog))
        (recur)))
    (def roi-filename (let [img-filename (.fileName (.getFileInfo imp))]
                        (str (.substring img-filename 0 (- (.length img-filename) 4))
                             "_plate.roi")))
    (def roi (ij1/get-roi imp))
    (.write (ij.io.RoiEncoder. roi-filename) roi))
  ; Load a ROI for testing
  (let [roi-filename "/Users/kharrington/Data/Harrington_Kyle/SanchezLab/ShapesOnPetriDish_plate.roi"]
    (def roi ^OvalRoi (ij.io.RoiDecoder/open roi-filename))))

; Interactive ROI selection, save ROI when done
; Could check if saved ROI exists, ask whether to load ROI or select a new one

(clear-outside-plate imp roi)

; Prompt for thresholds (this step should *BLOCK THREAD*)

(ij.IJ/setAutoThreshold imp "Default dark")
;(ij.IJ/run "Threshold...")
(def min-threshold (.getMinThreshold (.getProcessor imp)))
(def max-threshold (.getMaxThreshold (.getProcessor imp)))

; Fetch ROIs

(def rois (ij1seg/imp-to-rois imp))

; Color code ROIs by attribute

(defn get-area
  "Return the area of a ROI."
  [imp roi]
  (:area (ij1stats/get-image-statistics (ij1/set-roi imp roi)
                                        :area true)))

(defn get-solidity
  "Return the solidity of a ROI"
  [imp roi]
  (:solidity (ij1stats/get-image-statistics (ij1/set-roi imp roi)
                                            :solidity true)))

(defn get-convex-area
  "Return the solidity of a ROI"
  [imp roi]
  (:convex-area (ij1stats/get-image-statistics (ij1/set-roi imp roi)
                                               :convex-area true)))

;; Testing star measurement

(defn roi-to-imp
  "Return an imageplus mask of an ROI."
  [roi]
  #_(ij.ImagePlus. (.getMask roi))
  (let [rect ^java.awt.Rectangle (.getBounds roi)
        poly ^ij.process.FloatPolygon (.getFloatPolygon roi)
        imp (ij1/create-imp :width (.width rect) :height (.height rect) :type "8-bit")
        min-x (apply min (.xpoints poly))
        min-y (apply min (.ypoints poly))]
    (doall (for [x (range (ij1/get-width imp))
                 y (range (ij1/get-height imp))]
             (when (.contains poly
                     (+ min-x x)
                     (+ min-y y))
               (ij1/put-pixel-int imp x y 255))))
    imp))

;(ij1/show-imp (roi-to-imp (first rois)))

; Make a RGB image with the original roi, inscribed circle, and circumscribing circle
(defn roi-to-circ-imp
  "Return an imageplus mask of an ROI."
  [roi]
  #_(ij.ImagePlus. (.getMask roi))
  (let [rect ^java.awt.Rectangle (.getBounds roi)
        poly ^ij.process.FloatPolygon (.getFloatPolygon roi)
        max-dim (max (.width rect)
                     (.height rect))
        sq-dim (java.lang.Math/sqrt (* 2 (java.lang.Math/pow max-dim 2)))                                      
        imp (ij1/set-calibration (ij1/create-imp :width sq-dim :height sq-dim :type "8-bit")
                                 (ij.measure.Calibration.))
        min-x (apply min (.xpoints poly))
        min-y (apply min (.ypoints poly))
        offset-x (/ (- max-dim (.width rect)) 2)
        offset-y (/ (- max-dim (.height rect)) 2)
        ]
    (println max-dim (.width rect) (.height rect) offset-x offset-y min-x min-y)
    (doall (for [x (range (ij1/get-width imp))
                 y (range (ij1/get-height imp))]
             (when (.contains poly
                     (+ min-x x)
                     (+ min-y y))
               (ij1/put-pixel-int imp (+ offset-x x) (+ offset-y y) 255))))    
    (let [stats (ij1stats/get-image-statistics (ij1/set-roi imp roi) :center-of-mass true :centroid true)]
      (println (ij1/get-calibration (ij1/set-roi imp roi)))
      (println (.getX (ij1/get-calibration (ij1/set-roi imp roi)) 1))
      (println (:x-center-of-mass stats) (:y-center-of-mass stats))
      (println (:x-centroid stats) (:y-centroid stats))
      (ij1/set-roi imp (ij.gui.PointRoi. (:x-centroid stats) (:y-centroid stats))))))

#_(ij1/show-imp (roi-to-circ-imp (first rois)))

(defn get-roi-center-of-mass
  "Return the centroid of a ROI."
  [roi]
  (let [x-sum (atom 0)
        y-sum (atom 0)
        num-pixels (atom 0)
        rect ^java.awt.Rectangle (.getBounds roi)]
    (doall
      (for [x (range (.width rect))
            y (range (.height rect))]
        (when (.contains roi
                (int (+ (.x rect) x))
                (int (+ (.y rect) y)))
          (swap! num-pixels inc)
          (reset! x-sum (+ @x-sum (+ (.x rect) x)))
          (reset! y-sum (+ @y-sum (+ (.y rect) y))))))
    {:x (float (/ @x-sum @num-pixels))
     :y (float (/ @y-sum @num-pixels))}))

#_(let [roi (first rois)
       point-map (get-roi-center-of-mass roi)]
   (ij1/set-roi imp (ij.gui.PointRoi. (int (:x point-map)) (int (:y point-map))))) 

(defn get-roi-circumscribing-circle
  "Return a ROI representing the circumscribing circle centered at the center of mass for the ROI."
  [roi]
  (let [point-map (get-roi-center-of-mass roi)
        x-roi (.xpoints (.getPolygon roi))
        y-roi (.ypoints (.getPolygon roi))]     
    (loop [diameter 1]
      (let [circle-roi (ij.gui.OvalRoi. (int (- (:x point-map) (/ diameter 2))) (int (- (:y point-map) (/ diameter 2))) diameter diameter)]
        (if (reduce #(and %1 %2)
                    (map #(.contains circle-roi (int %1) (int %2))
                         x-roi y-roi))
          (let [diameter (dec diameter)]; Return the ROI with radius 1 smaller
            (ij.gui.OvalRoi. (int (- (:x point-map) (/ diameter 2))) (int (- (:y point-map) (/ diameter 2))) diameter diameter))
          (recur (inc diameter)))))))

#_(let [roi (first rois)
       circle-roi (get-roi-circumscribing-circle roi)]
   (println circle-roi)
   (ij1/set-roi imp circle-roi))

(defn get-pentagram-from-circle
  "Return a regular pentagram from a circle roi."
  [circle-roi]
  (let [width (int (.getFloatWidth circle-roi))
        circumradius (/ width 2)
        xc (int (+ circumradius (.getXBase circle-roi)))
        yc (int (+ circumradius (.getYBase circle-roi)))
        inner-radius (* (/ circumradius 0.525731) 0.200811)]
    (ij.gui.PolygonRoi. (float-array (map #(+ xc %)
                                          (interleave (map #(* circumradius (java.lang.Math/cos (* % 2 (/ java.lang.Math/PI 5))))
                                                          (range 5)); Outer points
                                                      (map #(* inner-radius (java.lang.Math/cos (+ (/ java.lang.Math/PI 5) (* % 2 (/ java.lang.Math/PI 5)))))
                                                           (range 5))))); inner points
                        (float-array (map #(+ yc %)
                                          (interleave (map #(* circumradius (java.lang.Math/sin (* % 2 (/ java.lang.Math/PI 5))))
                                                           (range 5)); Outer points
                                                     (map #(* inner-radius (java.lang.Math/sin (+ (/ java.lang.Math/PI 5) (* % 2 (/ java.lang.Math/PI 5)))))
                                                          (range 5))))); inner points
                        (ij.gui.Roi/POLYGON))))

#_(let [roi (first rois)
       circle-roi (get-roi-circumscribing-circle roi)
       pentagram-roi (get-pentagram-from-circle circle-roi)]
   (println pentagram-roi)
   (ij1/set-roi imp pentagram-roi))

(defn score-roi-overlap
  "Return the binary scores for ROI overlaps (target is truth, candidate is prediction)"
  [^ij.gui.Roi target ^ij.gui.Roi prediction]
  (let [target-rect (.getBounds target)
        pred-rect (.getBounds prediction)
        min-x (min (.x target-rect) (.x pred-rect))
        min-y (min (.y target-rect) (.y pred-rect))
        max-x (max (+ (.x target-rect) (.width target-rect))
                   (+ (.x pred-rect) (.width pred-rect)))
        max-y (max (+ (.y target-rect) (.height target-rect))
                   (+ (.y pred-rect) (.height pred-rect)))
        tp (atom 0)
        fp (atom 0)
        tn (atom 0)
        fn (atom 0)]
    (doall
      (for [x (range min-x (inc max-x))
            y (range min-y (inc max-y))]
        (let [in-target (.contains target x y)
              in-pred (.contains prediction x y)]
          (cond (and in-target in-pred) (swap! tp inc)
                (and in-target (not in-pred)) (swap! fn inc)
                (and (not in-target) in-pred) (swap! fp inc)
                (and (not in-target) (not in-pred)) (swap! tn inc)))))
    {:tp @tp
     :tn @tn
     :fp @fp
     :fn @fn
     :accuracy (if (zero? (+ @tp @tn @fp @fn))
                 0
                 (float (/ (+ @tp @tn) 
                           (+ @tp @tn @fp @fn))))
     :precision (if (zero? (+ @tp @fp))
                  0
                  (float (/ @tp
                            (+ @tp @fp))))
     :recall (if (zero? (+ @tp @fn))
               0
               (float (/ @tp (+ @tp @fn))))
     :f1 (if (or (zero? (+ @tp @fp))
                 (zero? (+ @tp @fn))
                 (zero? (+ (/ @tp 
                                (+ @tp @fp)) 
                             (/ @tp (+ @tp @fn)))))
           0
           (float (* 2 (/ (* (/ @tp 
                                (+ @tp @fp)) 
                             (/ @tp 
                                (+ @tp @fn)))
                          (+ (/ @tp 
                                (+ @tp @fp)) 
                             (/ @tp (+ @tp @fn)))))))}))
          
#_(let [roi (first rois)
       circle-roi (get-roi-circumscribing-circle roi)
       pentagram-roi (get-pentagram-from-circle circle-roi)]
   (println pentagram-roi)
   (println (score-roi-overlap pentagram-roi roi))
   (ij1/set-roi imp pentagram-roi))

(defn get-best-pentagram-roi
  "Return the ROI representing the best matching pentagram for a given ROI."
  [imp roi show-matching]
  (let [circle-roi (get-roi-circumscribing-circle roi)
        pentagram-roi (get-pentagram-from-circle circle-roi)]
    (when show-matching
      (ij1/set-roi imp pentagram-roi))
    (loop [angle 0
           current-roi pentagram-roi
           best-pentagram (.clone pentagram-roi)
           best-scores (score-roi-overlap pentagram-roi roi)]
      (if (< angle 72);; Only have to do 72 degrees because of symmetry
        (let [current-roi (ij.plugin.RoiRotator/rotate current-roi 1)]
          (when show-matching
            (ij1/set-roi imp current-roi))
          (let [new-scores (score-roi-overlap (ij1/get-roi imp) roi)]            
            (if (> (:f1 new-scores) (:f1 best-scores))
              (recur (inc angle)
                     current-roi
                     (.clone current-roi)
                     new-scores)
              (recur (inc angle)
                     current-roi
                     best-pentagram
                     best-scores))))
        (assoc best-scores
               :angle angle
               :roi best-pentagram)))))

;IJ.run(imp, "Rotate...", "  angle=1");

#_(let [roi (first rois)
       pentagram-map (get-best-pentagram-roi imp roi)]
   (println pentagram-map)
   (ij1/set-roi imp (:roi pentagram-map)))

(defn get-pentagram-score
  "Return the F1 score for the best matching pentagram."
  [imp roi]
  (let [pentagram-map (get-best-pentagram-roi imp roi true)]
    ;(println "pentagram: " (:f1 pentagram-map))
    (if (:f1 pentagram-map)
      (:f1 pentagram-map)
      0)))

(def measures
  {:perimeter (fn [imp roi] (.getLength roi))
   :area get-area
   :solidity get-solidity
   ;:inverse-solidity #(/ (get-solidity %1 %2))
   :pentagram get-pentagram-score
   :convex-area get-convex-area
   })

(doseq [[measure-name measure-fn] measures]
  (let [imp-cc (ij1/autocontrast (ij1seg/color-code-rois imp rois (partial measure-fn imp)))]    
    (ij.IJ/run imp-cc "Fire" "")
    (ij1/save-imp-as-tiff (ij1/show-imp (ij1/set-title imp-cc (name measure-name)))
                          (let [img-filename (.fileName (.getFileInfo imp))]
                            (str (.substring img-filename 0 (- (.length img-filename) 4)) "_" (name measure-name) ".tif")))))
