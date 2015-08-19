(ns funimage.segmentation.imp
 (:use [funimage imp project]
       [funimage.imp threshold calculator roi]
       [funimage.segmentation utils])
 (:require [clojure.string :as string])
 (:import [mpicbg.imglib.image Image]
          [mpicbg.imglib.cursor Cursor]
          [script.imglib ImgLib]
          [mpicbg.imglib.type.numeric NumericType]
          [ij IJ ImagePlus WindowManager]
          [ij.measure ResultsTable]
          [ij.gui WaitForUserDialog PointRoi NewImage PolygonRoi Roi GenericDialog NonBlockingGenericDialog Line]
          [ij.plugin ImageCalculator Duplicator]
          [ij.plugin.frame RoiManager]
          [ij.process ImageConverter FloatProcessor ByteProcessor]
          [java.awt Button]
          [java.awt.event ActionListener]
          [java.io File]
          #_[weka.clusterers SimpleKMeans]
          #_[weka.core Instances Attribute DenseInstance]))






(defn analyze-particles
  "Use the ImageJ particle analyzer."
  [imp & args]
  (let [argmap (apply hash-map args)
        min-size (or (:min-size argmap) 0)
        max-size (or (:max-size argmap) "Infinity")
        return-mask? (or (:return-mask argmap) false)]
    ;(println "Analyze particles arguments: " (str "size=" min-size "-" max-size " display clear add"))
    (IJ/run imp "Analyze Particles..." (str "size=" min-size "-" max-size " display clear add"))
    (cond return-mask?
          (let [rois (get-rois)
                mask (create-imp-like imp)]
            (set-fill-value mask 255)
            (doseq [roi rois]
              (.fillPolygon ^ij.process.ImageProcessor (.getProcessor mask) ^java.awt.Polygon (.getPolygon roi)))
            (imp-and mask imp))
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

(defn imp-to-segments
   "Get the segments from an imp (kidney meca specialized)."
   [imp]
   ;(let [imp (first (split-channels imp))]
     (.setSnapshotCopyMode (.getProcessor imp) false)
     (IJ/run imp "Invert" "");
     ;(IJ/run imp "Auto Threshold" "method=Otsu white");
     (autothreshold imp :otsu false false true false false false)     
     (let [prev-title (.getTitle imp)]
       (IJ/run imp "Analyze Particles..." "size=8000-Infinity display clear add");
       (let [min-size 4000 ; was 2000
             roi-manager (get-roi-manager)
             particle-imp (create-imp-like imp)]
         (.setSnapshotCopyMode (.getProcessor particle-imp) false)
         (set-fill-value particle-imp 255)
         (doseq [roi (.getRoisAsArray roi-manager)]
           (let [bounds (.getBounds roi)]
             (.fillPolygon ^ij.process.ImageProcessor (.getProcessor particle-imp) ^java.awt.Polygon (.getPolygon roi))
             #_(when (> (* (.getWidth bounds) (.getHeight bounds)) min-size); from ASN
                #_(set-roi particle-imp roi)
                (.fillPolygon ^ij.process.ImageProcessor (.getProcessor particle-imp) ^java.awt.Polygon (.getPolygon roi))
                #_(fill particle-imp))))
         particle-imp)))

#_(defn kmeans-clustering
   "K-means clustering on the results table."
   [result-table num-clusters]
   (let [clusterer (SimpleKMeans.)
         ignore-headings ["X" "Y" "XM" "YM" "FeretX" "FeretY"]
         header (filter #(when-not (some #{%} ignore-headings) %) (.getHeadings result-table))
         attributes (map #(Attribute. %) header)
         instances (Instances. "Particles" (java.util.ArrayList. attributes) 0)]
     (IJ/log "Clustering with features: ")
     (doseq [h header] (IJ/log h))
     (.setNumClusters clusterer num-clusters)
     (.setPreserveInstancesOrder clusterer true)
     (dotimes [row-idx (.size result-table)]
       (let [inst (DenseInstance. (count header))]
         (dotimes [col-idx (count header)]
			       (.setValue inst (nth attributes col-idx)
				       (.getValue result-table (nth header col-idx) (int row-idx))))
		     (.add instances inst)))
	   (.buildClusterer clusterer instances)
	   (let [cluster-labels (.getAssignments clusterer)]
		   (dotimes [row-idx (.size result-table)]
			   (.setValue result-table "Cluster" row-idx (double (nth cluster-labels row-idx))))))); Should give unique names

#_(defn kmeans-clustering
     "K-means clustering on the results table."
     ([result-table num-clusters]
       (let [ignore-headings ["X" "Y" "XM" "YM" "FeretX" "FeretY"]
             header (filter #(when-not (some #{%} ignore-headings) %) (.getHeadings result-table))]
         (kmeans-clustering result-table num-clusters header)))
     ([result-table num-clusters header]
       (let [clusterer (SimpleKMeans.)         
             attributes (map #(Attribute. %) header)
             instances (Instances. "Particles" (java.util.ArrayList. attributes) 0)
             column-name (str (gensym "cluster"))]
         (IJ/log "Clustering with features: ")
         (doseq [h header] (IJ/log h))
         (.setNumClusters clusterer num-clusters)
         (.setPreserveInstancesOrder clusterer true)
         (dotimes [row-idx #_(.size result-table) (.getCounter result-table)]
           (let [inst (DenseInstance. (count header))]
             (dotimes [col-idx (count header)]
			           (.setValue inst (nth attributes col-idx)
				           (.getValue result-table (nth header col-idx) (int row-idx))))
		         (.add instances inst)))
	       (.buildClusterer clusterer instances)
	       (let [cluster-labels (.getAssignments clusterer)]
		       (dotimes [row-idx #_(.size result-table) (.getCounter result-table)]
			       (.setValue result-table column-name row-idx (double (nth cluster-labels row-idx)))))
        column-name)))

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
     (spit (str (project-directory) "/" basename "_" measure "_hist.csv")
           (string/join "\n"
                        (map #(string/join "\t" %)
                             (map list 
                                  (map #(float %) (range hist-min hist-max hist-step))
                                  hist
                                  norm-hist)))))

   (IJ/selectWindow (str "Histogram of " measure))
   (IJ/saveAs (IJ/getImage) "Tiff" (str (project-directory) "/" basename "_" measure "_hist.tif"))
   (.close (IJ/getImage)))))

#_(let [hist (range 10)
       shift -2]
   (if (neg? shift)
     (concat (drop (+ (count hist) shift) hist) (take (+ (count hist) shift) hist))
     (concat (drop shift hist) (take shift hist))))
         
(defn segment-attribute-color-coding
  "Color code segments by an attribute."
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


