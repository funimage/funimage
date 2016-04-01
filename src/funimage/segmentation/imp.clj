(ns funimage.segmentation.imp
 (:use [funimage imp]
       [funimage.imp threshold calculator roi]
       [funimage.segmentation utils])
 (:require [clojure.string :as string])
 (:import [ij IJ ImagePlus WindowManager]
          [ij.measure ResultsTable]
          [ij.gui WaitForUserDialog PointRoi NewImage PolygonRoi Roi GenericDialog NonBlockingGenericDialog Line]
          [ij.plugin ImageCalculator Duplicator]
          [ij.plugin.frame RoiManager]
          [ij.process ImageConverter FloatProcessor ByteProcessor]
          [java.awt Button]
          [java.awt.event ActionListener]
          [java.io File]))

; (with-measurements m & body)

(defn analyze-particles
  "Use the ImageJ particle analyzer."
  [imp & args]
  (let [argmap (apply hash-map args)
        min-size (or (:min-size argmap) 0)
        max-size (or (:max-size argmap) "Infinity")
        ;return-mask? (or (:return-mask argmap) false)        
        result-type (or (:return-type argmap) :original) ; :original, :mask, :labeled        
        method-args (str (when (or (:clear-results argmap) false) " clear")
                         (when (or (= result-type :results)
                                   (:display-results argmap)
                                   false)
                           " display")
                         (when (or (:add-results argmap) false) " add"))
        ]
    ;(println "Analyze particles arguments: " (str "size=" min-size "-" max-size " display clear add"))
    (IJ/run imp "Analyze Particles..." (str "size=" min-size "-" max-size method-args #_" display clear add"))
    (cond (= result-type :mask)
          (let [rois (get-rois)
                mask (create-imp-like imp)]            
            (doseq [roi rois]
              (set-fill-value mask 255)
              (.fillPolygon ^ij.process.ImageProcessor (.getProcessor mask) ^java.awt.Polygon (.getPolygon roi)))
            (imp-and mask imp))
          (= result-type :labeled)
          (let [rois (get-rois)
                mask (create-imp-like imp)]            
            ;(doseq [roi rois]
            (dotimes [k (count rois)]
              (let [roi (nth rois k)]
                (set-fill-value mask k)
                (.fillPolygon ^ij.process.ImageProcessor (.getProcessor mask) ^java.awt.Polygon (.getPolygon roi))))
            mask)
          (= result-type :results)
          (let [rt (get-results-table)]
            (results-table-to-map rt))
          :else imp)))

#_(defn size-filter-stack
   "Use the ImageJ particle analyzer."
   [imp & args]
   (let [argmap (apply hash-map args)
         min-size (or (:min-size argmap) 0)
         max-size (or (:max-size argmap) "Infinity")
         return-mask? true
         rois (atom [])
         stack ^ij.ImageStack (.getImageStack imp)
         output (create-imp-like imp)
         outstack ^ij.ImageStack (.getImageStack output)]
     (dotimes [idx (.getSize stack)]
       (let [ip ^ij.process.ImageProcessor (.getProcessor stack (inc idx))
             w ^ij.gui.Wand (ij.gui.Wand. zipmap)]
         (.setAllPoints w true)
         (dorun (for [x (get-width imp) y (get-height imp)]
                  (do (.autoOutline w x y 0 256)
                    (let [roi ^ij.gui.Roi (ij.gui.PolygonRoi. (.xpoints w) (.ypoints w) (.npoints w) ij.gui.Roi/FREEROI)
                          r (.getBounds roi)]
                      ;(when (and (> (.width r) 1) (> (.height r) 1))
                     (when (and  (>= (* (.width r) (.height r)) min-size) (>= (* (.width r) (.height r)) max-size))
                       (.fillPolygon
                         ^ij.process.ImageProcessor (.getProcessor outstack (inc idx))
                         ^java.awt.Polygon (.getPolygon roi))
                       (swap! rois conj roi))))))))
     output))

(defn save-results-table-as-csv
  "Save the entire results table to a CSV file."
  [result-table filename]
  (let [headings (.getHeadings result-table)]
	  (spit #_(str (project-directory) "/particle-data.csv")
         filename
         (with-out-str
           ; Headings
           (doseq [heading headings]
		          (when-not (= heading (last headings)) (print "\t")))
           (println)
           ; Rows of data
           (dotimes [row-idx #_(.size result-table) (.getCounter result-table)]
		          (dotimes [col-idx (count headings)]
              (when-not (= col-idx (dec (count headings))) (print "\t")))
		          (println))))))

#_(defn imp-to-segments
   "Get the segments from an imp (kidney meca specialized)."
   [imp]
   (IJ/run imp "16-bit" "");
   (IJ/run imp "Invert" "");
   ;(IJ/run imp "Auto Threshold" "method=Otsu white");
   (autothreshold imp :otsu false false true false false false)
   (IJ/saveAs imp "Tiff" (str (project-directory) "/particles.tif"))
   imp
   #_(IJ/getImage)  
   #_(let [prev-title (.getTitle imp)]
      (IJ/run imp "Analyze Particles..." "size=25-Infinity show=Masks display clear add");
      (IJ/selectWindow (str "Mask of " prev-title))
      (IJ/saveAs (IJ/getImage) "Tiff" (str (project-directory) "/particles.tif"))
      (IJ/getImage)))

(defn segment-attribute-histogram
  "Get a histogram for a given attribute of all segments."
  ([result-table measure]
    (segment-attribute-histogram result-table measure ""))
  ([result-table measure basename]
	(let [measure-imp (NewImage/createFloatImage measure
                                              #_(.size result-table) (.getCounter result-table) 1 1 NewImage/FILL_BLACK)
       proc (.getProcessor measure-imp)
       hist-lines (atom [])
       differences (atom 0)
       binary-differences (atom 0)
       ;nbins 1000
       nbins 10
       hist-min 0
       hist-max (cond (= measure "Round") 1
                      (= measure "Angle") 180
                      (= measure "Density_50") 3000000
                      :else (apply max (for [k (range #_(.size result-table) (.getCounter result-table))] (.getValue result-table measure k))))
       result-table (get-results-table)]
   (dotimes [k #_(.size result-table) (.getCounter result-table)]
     (let [value (.getValue result-table measure k)]
       (.putPixelValue proc k 0 value)))
   (IJ/run measure-imp "Histogram" (str "bins=" nbins " use x_min=" hist-min
                                        "  x_max=" hist-max " y_max=Auto"))
   (let [stats (.getStatistics measure-imp 0 nbins hist-min hist-max)
         hist (.getHistogram stats)
         hist (if (= measure "Angle")
                (let [max-val (apply max hist)
                      max-idx (some #(when (= (second %) max-val) (first %))
                                    (map list (range) hist))
                      shift (- max-idx (int (/ (count hist) 2))) #_(- (int (/ (count hist) 2)) max-idx)]
#_(println shift (seq hist))                  
                  (if (neg? shift)
                    (concat (drop (+ (count hist) shift) hist) (take (+ (count hist) shift) hist))
                    (concat (drop shift hist) (take shift hist))))
                hist)
;         tmp (println measure hist)
         hist-sum (apply + hist)
         norm-hist (map #(float (/ % hist-sum)) hist)
         hist-step (.binSize stats)]
     #_(spit (str (project-directory) "/" basename "_" measure "_hist.csv")
            (string/join "\n"
                         (map #(string/join "\t" %)
                              (map list 
                                   (map #(float %) (range hist-min hist-max hist-step))
                                   hist
                                   norm-hist)))));; This is the data you want!!!!!

   #_(IJ/selectWindow (str "Histogram of " measure))
   #_(IJ/saveAs (IJ/getImage) "Tiff" (str (project-directory) "/" basename "_" measure "_hist.tif"))
   #_(.close (IJ/getImage))
   (IJ/getImage)))); return data instead of an image.

#_(let [hist (range 10)
       shift -2]
   (if (neg? shift)
     (concat (drop (+ (count hist) shift) hist) (take (+ (count hist) shift) hist))
     (concat (drop shift hist) (take shift hist))))
  
; Works, but relies on BAR
#_(defn segment-attribute-color-coding
   "Color code segments by an attribute. Requires BAR plugin. Consider using segment-attribute-imp with a LUT"
   [measure imp lut]; Round, Angle
   (IJ/selectWindow (.getTitle imp) #_"particles.tif")
   (let [minval 0
         result-table (get-results-table)
         maxval (cond (= measure "Round") 1
                      (= measure "Angle") 180
                      :else (apply max (for [k (range #_(.size result-table) (.getCounter result-table))] (.getValue result-table measure k))))]
     (IJ/run imp "ROI Color Coder" (str "measurement=" measure " lut=[" lut "] width=0 opacity=80 label=micron^2 range=" ;Ice, Spectrum
												   minval "-" maxval " n.=5 decimal=0 ramp=[256 pixels] font=SansSerif font_size=14 draw")))
   ;(.runCommand roi-manager "show all without labels")
   (IJ/saveAs (IJ/getImage) "Tiff" (str (project-directory) "/" measure "_legend.tif"))
   (.close (IJ/getImage)); close legend
   (.runCommand (get-roi-manager) "show all without labels")
   (IJ/run "Flatten")
   (IJ/saveAs (IJ/getImage) "Tiff" (str (project-directory) "/" measure ".tif"))
   (IJ/getImage)
   #_(.close (IJ/getImage)))

#_(defn segment-attribute-imp
   "Return an imp with segments labeled based on a measure.
You need to run analyze particles first."
   [measure imp]
   (let [result-table (get-results-table)
         roi-manager (get-roi-manager)
         new-imp #_(copy-imp imp)
         (create-imp :title (str measure "_" (get-title imp))
                     :type "32-bit"
                     :width (get-width imp)
                     :height (get-height imp))
         rois (.getRoisAsArray roi-manager)]
     (dotimes [k (.getCount roi-manager)]
       (let [roi-name (.getName roi-manager k)
             result-idx (read-string (second (string/split roi-name #"-")))]
         (set-roi new-imp (nth rois k))
         (set-fill-value new-imp (.getValue result-table measure k))
         (fill new-imp)
         #_(println k result-idx)))
     (set-roi new-imp nil)
     new-imp))

(defn segment-attribute-imp
  "Return an imp with segments labeled based on a measure.
You need to run analyze particles first."
  [measure imp]
  (let [result-table (get-results-table)
        roi-manager (get-roi-manager)
        new-imp #_(copy-imp imp)
        (create-imp :title (str measure "_" (get-title imp))
                    :type "32-bit"
                    :width (get-width imp)
                    :height (get-height imp))
        rois (.getRoisAsArray roi-manager)
        min-size 2000]
    (dotimes [k (.getCount roi-manager)]
      (let [roi-name (.getName roi-manager k)
            result-idx (bigint (string/trim (second (string/split roi-name #"-"))))
            roi (nth rois k)
            bounds (.getBounds roi)]
        (when (> (* (.getWidth bounds) (.getHeight bounds)) min-size)
          ;(set-roi new-imp (nth rois k))
          (set-fill-value new-imp (.getValue result-table measure k))
          (.fillPolygon ^ij.process.ImageProcessor (.getProcessor new-imp) ^java.awt.Polygon (.getPolygon roi))
          ;(fill new-imp)
          #_(println k result-idx))))
    new-imp))

(defn color-code-rois
  "Return an imp with segments labeled based on a measure.
You need to run analyze particles first."
  [imp roi-maps key-fn]
  (let [new-imp #_(copy-imp imp)
        (create-imp :title (str (name key-fn) "_" (get-title imp))
                    :type "32-bit"
                    :width (get-width imp)
                    :height (get-height imp))]
    (dotimes [k (count roi-maps)]; This can be a for now
      (let [roi-map (nth roi-maps k)
            roi (:roi roi-map)
            bounds (.getBounds roi)
            v (key-fn roi-map)]
        ;(set-fill-value new-imp v)        
        (.setValue ^ij.process.ImageProcessor (.getProcessor new-imp) v)
        (.setColor ^ij.process.ImageProcessor (.getProcessor new-imp) v)
        (.fillPolygon
          ^ij.process.ImageProcessor (.getProcessor new-imp)
          ^java.awt.Polygon (.getPolygon roi))))
    new-imp))

; color coded legend  with histogram
; density of neighboring particles
; density of neighboring particles weighted by area coverage
			
;(spit (str (project-directory) "/params.clj") @params)

;(IJ/run "Close All" "")



#_(defn closest-segment-map
   "Return a map where every pixel contains the value stored in the closest segment.
This is particularly useful when combined with segment-attribute-imp. 
Uses centroids"
   [measure imp]
   (let [result-table (get-results-table)
         new-imp (create-imp :title (str measure "_" (get-title imp))
                     :type "32-bit"
                     :width (get-width imp)
                     :height (get-height imp))
         img (imp->img new-imp)
         pixel-width 0.25
         pixel-height 0.25]
     (walk-imgs (fn [^Cursor cur]
                  (let [this-x (* pixel-width (.getLongPosition cur 0))
                        this-y (* pixel-height (.getLongPosition cur 1))
                        closest-idx (ffirst (sort-by second (map #(list %
                                                                        (java.lang.Math/sqrt (+ (java.lang.Math/pow (- this-x (.getValue result-table "X" %)) 2) 
                                                                                                (java.lang.Math/pow (- this-y (.getValue result-table "Y" %)) 2))))
                                                                 (range (.getCounter result-table)))))]
                    (.set (.get cur) (float (.getValue result-table measure closest-idx)))))
                img)
     [new-imp (img->imp img)]))
  
#_(defn closest-segment-map
    "Return a map where every pixel contains the value stored in the closest segment.
This is particularly useful when combined with segment-attribute-imp. 
Uses centroids"
    [measure imp]
    (let [result-table (get-results-table)
          new-imp (create-imp :title (str measure "_" (get-title imp))
                      :type "32-bit"
                      :width (get-width imp)
                      :height (get-height imp))
          img (imp->img new-imp)
          pixel-width 0.25
          pixel-height 0.25
          measures (for [k (range (.getCounter result-table))] (.getValue result-table measure k))
          positions (for [k (range (.getCounter result-table))] (net.imglib2.RealPoint. (double-array [(.getValue result-table "X" k) (.getValue result-table "Y" k)])))
          kd-tree (net.imglib2.KDTree. measures positions)]
      (walk-imgs (fn [^net.imglib2.Cursor cur]
                   (let [this-x (* pixel-width (.getLongPosition cur 0))
                         this-y (* pixel-height (.getLongPosition cur 1))
                         closest-idx (ffirst (sort-by second (map #(list %
                                                                         (java.lang.Math/sqrt (+ (java.lang.Math/pow (- this-x (.getValue result-table "X" %)) 2) 
                                                                                                 (java.lang.Math/pow (- this-y (.getValue result-table "Y" %)) 2))))
                                                                  (range (.getCounter result-table)))))]
                     (.set (.get cur) (float (.getValue result-table measure closest-idx)))))
                 img)
      [new-imp (img->imp img)]))

#_(defn stack-to-regions-2d
   "Size filter an image slice by slice over the whole stack."
   [imp & args]
   (let [argmap (apply hash-map args)
         regions (atom [])
         stack ^ij.ImageStack (.getImageStack imp)
         history ^ij.ImagePlus (create-imp-like imp)
         history-stack ^ij.ImageStack (.getImageStack history)]
     (dotimes [idx (.getSize stack)]
       (let [ip ^ij.process.ImageProcessor (.getProcessor stack (inc idx))
             hip ^ij.process.ImageProcessor (.getProcessor history-stack (inc idx))
             w ^ij.gui.Wand (ij.gui.Wand. ip)]
         (doall (for [x (range (get-width imp)) y (range (get-height imp))]
                  (when (and (zero? (.getPixel hip x y))
                             #_(> (.getPixel ip x y) 0)); could throw out contiguous 0s from search as well
                    (.autoOutline w (int x) (int y) (int 1) (int 256))
                    (let [roi ^ij.gui.Roi (ij.gui.PolygonRoi. (.xpoints w) (.ypoints w) (.npoints w) ij.gui.Roi/FREEROI #_ij.gui.Roi/POLYGON)
                          perim (.getLength roi)
                          area (java.lang.Math/pow (/ perim (* 2 java.lang.Math/PI)) 2)
                          r ^java.awt.Rectangle (.getBounds roi)]
                      (set-fill-value history 255)
                      (.fillPolygon ^ij.process.ImageProcessor hip ^java.awt.Polygon (.getPolygon roi))
                      (swap! regions conj {:roi roi
                                           :z idx
                                           :area area})))))))
     @regions))

(defn imp-to-rois
  "Return a collection of ROIs from an imp."
  [^ImagePlus imp & args]
  (let [argmap (apply hash-map args)
        min-size (or (:min-size argmap) 0)
        max-size (or (:max-size argmap) "Infinity")
        ;return-mask? (or (:return-mask argmap) false)        
        result-type (or (:return-type argmap) :original) ; :original, :mask, :labeled        
        method-args (str (when (or (:clear-results argmap) false) " clear")
                         (when (or (= result-type :results)
                                   (:display-results argmap)
                                   false)
                           " display")
                         " exclude add")
        manager (ij.plugin.frame.RoiManager. true)]
    (ij.plugin.filter.ParticleAnalyzer/setRoiManager manager)
    (ij.IJ/run imp "Analyze Particles..." method-args)
    (.getRoisAsArray manager)))

;(defn roi-intersects-roi?
;  "Predicate: does one ROI intersect another."
;  [roi-source roi-candidate]
;  (not= (.npoints roi-source)
;        (count (keep #(.contains roi-source (

#_(defn imp-to-rois2
   "Size filter an image slice by slice over the whole stack."
   [imp & args]
   (let [argmap (apply hash-map args)
         min-size (or (:min-size argmap) 0)
         max-size (or (:max-size argmap) "Infinity")
         max-size (if (string? max-size) java.lang.Integer/MAX_VALUE max-size)
         rois (atom [])
         imp (copy-imp imp); Don't damage the input, oh so lazy
         stack ^ij.ImageStack (.getImageStack imp)]
     (dotimes [idx (.getSize stack)]
       (let [ip ^ij.process.ImageProcessor (.getProcessor stack (inc idx))
             w ^ij.gui.Wand (ij.gui.Wand. ip)]
         (doall (for [x (range (get-width imp)) y (range (get-height imp))]
                  (when (> (.getPixel ip x y) 0); could throw out contiguous 0s from search as well
                    (.autoOutline w (int x) (int y) (int 1) (int 256))                   
                    (let [roi ^ij.gui.Roi (ij.gui.PolygonRoi. (.xpoints w) (.ypoints w) (.npoints w) ij.gui.Roi/FREEROI #_ij.gui.Roi/POLYGON)
                          perim (.getLength roi)
                          area (java.lang.Math/pow (/ perim (* 2 java.lang.Math/PI)) 2)
                          r ^java.awt.Rectangle (.getBounds roi)]
                      (.setColor ip 0)
                      (.fillPolygon
                         ip
                         ^java.awt.Polygon (.getPolygon roi))
                      (swap! rois conj roi)))))))
     ;; We may have too many ROIs. Throw away ROIs until none intersect.
     (loop [candidates @rois
            non-intersecting []]
       (if (empty? candidates)
         non-intersecting
         (recur (keep (map #(and (not (roi-intersects-roi? (first candidates) %)) %); Uses Emma Tosch's logical operator shorthand
                           candidates)); Only keep candidates that do not intersect with the current candidate
                (concat non-intersecting
                        (conj non-intersecting (first candidates))))))))

(defn rois-to-imp
  "Render a collection of rois as an image. Currently defaults to binary, will change.
This function writes to imp. Only works on 1 slice."
  [imp rois & args]
  (doseq [roi rois]
    ;(println roi)
    (.setColor ^ij.process.ImageProcessor (.getProcessor imp)  255)
    (.fillPolygon
       ^ij.process.ImageProcessor (.getProcessor imp) 
       ^java.awt.Polygon (.getPolygon roi)))
  imp)

