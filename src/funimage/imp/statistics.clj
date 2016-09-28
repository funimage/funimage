(ns funimage.imp.statistics  
  (:require [clojure.string :as string]
            [clojure.java.io :as io])
  (:import [java.io File]
           [ij IJ ImagePlus ImageStack]

           [ij.gui NewImage Toolbar Roi]
           [ij.process ImageProcessor ByteProcessor ImageStatistics]
           [ij.measure Calibration]
           [ij.plugin Duplicator]
           
           [ij.io Opener])
  (:use [funimage imp]))

(defn get-available-measurements
  "Return a sequence of keywords corresponding to available measurements."
  []
  #{:area  :area-fraction  :center-of-mass  :centroid  :circularity  :ellipse  :feret  :integrated-density  :invert-y  
   :kurtosis  :labels  :limit  :max-standards  :mean  :median  :min-max  :mode  :nan-empty-cells  :perimeter  :rect  
   :scientific-notation  :shape-descriptors  :skewness  :slice  :stack-position  :std-dev})
; :add-to-overlay

(defn enable-measurements
  "Enable the list of measurements."
  [to-enable]
  (ij.IJ/run "Set Measurements..." (str "redirect=None decimal=3 " (string/join " " 
                                                                                (map #(str (name %)) to-enable))))) 

(defn get-polygon-area
  "Return the area of a polygon."
  [p]
  (if (nil? p)
    Double/NaN
    (let [carea (atom 0)]
      (dotimes [i (.npoints p)]
        (let [iminus1 (if (< (dec i) 0)
                        (dec (.npoints p))
                        (dec i))]
          (reset! carea
                 (+ @carea
                    (* (+ (aget (.xpoints p) i)
                          (aget (.xpoints p) iminus1))
                       (- (aget (.ypoints p) i)
                          (aget (.ypoints p) iminus1)))))))
      (java.lang.Math/abs (/ @carea 2.0)))))

(defn get-image-statistics
  "Return a map of image statistics."
  [^ImagePlus imp & args]
  (let [argmap (apply hash-map args)
        measurement-options
        (apply + 
               (for [[k v] argmap]
                 (cond (= k :add-to-overlay) ij.measure.Measurements/ADD_TO_OVERLAY 
                       (= k :area) ij.measure.Measurements/AREA
                       (= k :area-fraction) ij.measure.Measurements/AREA_FRACTION
                       (= k :center-of-mass) ij.measure.Measurements/CENTER_OF_MASS
                       (= k :centroid) ij.measure.Measurements/CENTROID
                       (= k :circularity) ij.measure.Measurements/CIRCULARITY
                       (= k :ellipse) ij.measure.Measurements/ELLIPSE
                       (= k :feret) ij.measure.Measurements/FERET
                       (= k :integrated-density) ij.measure.Measurements/INTEGRATED_DENSITY
                       (= k :invert-y) ij.measure.Measurements/INVERT_Y
                       (= k :kurtosis) ij.measure.Measurements/KURTOSIS
                       (= k :labels) ij.measure.Measurements/LABELS
                       (= k :limit) ij.measure.Measurements/LIMIT
                       (= k :max-standards) ij.measure.Measurements/MAX_STANDARDS
                       (= k :mean) ij.measure.Measurements/MEAN
                       (= k :median) ij.measure.Measurements/MEDIAN
                       (= k :min-max) ij.measure.Measurements/MIN_MAX
                       (= k :mode) ij.measure.Measurements/MODE
                       ;(= k :nan-empty-cells) ij.measure.Measurements/NaN_EMPTY_CELLS
                       (= k :perimeter) ij.measure.Measurements/PERIMETER
                       (= k :rect) ij.measure.Measurements/RECT
                       (= k :scientific-notation) ij.measure.Measurements/SCIENTIFIC_NOTATION
                       (= k :shape-descriptors) ij.measure.Measurements/SHAPE_DESCRIPTORS
                       (= k :skewness) ij.measure.Measurements/SKEWNESS
                       (= k :slice) ij.measure.Measurements/SLICE
                       (= k :stack-position) ij.measure.Measurements/STACK_POSITION
                       (= k :std-dev) ij.measure.Measurements/STD_DEV)))
        measurement-options (if measurement-options measurement-options 0)
        measurement-options (cond (and (contains? argmap :solidity) 
                                       (contains? argmap :area))
                                  (+ measurement-options ij.measure.ResultsTable/SOLIDITY)
                                  (and (contains? argmap :solidity) 
                                       (not (contains? argmap :area)))
                                  (+ measurement-options ij.measure.ResultsTable/SOLIDITY
                                     ij.measure.Measurements/AREA)
                                  :else
                                  measurement-options)
        ^ij.process.ImageStatistics stats (ij.process.ImageStatistics/getStatistics (.getProcessor imp) measurement-options (.getCalibration imp))]
    {:angle (.angle stats)
     :area (.area stats)
     :area-fraction (.areaFraction stats)
     :bin-size (.binSize stats)
     :perimeter (if (.getRoi imp) 
                  (.getLength ^ij.gui.Roi (.getRoi imp))
                  (+ (* 2 (get-width imp)) (* 2 (get-height imp)))) 
     :circularity (if (.getRoi imp)
                    (* 4.0 java.lang.Math/PI (/ (.area stats) (apply * (repeat 2 (when (.getRoi imp) (.getLength ^ij.gui.Roi (.getRoi imp)))))))
                    1) 
     :convex-area (when (get-roi imp) (get-polygon-area (.getConvexHull (get-roi imp))))
     ;:cal (.cal stats); should probably expand calibration
     :dmode (.dmode stats)
     ;:height (.height stats)
     :hist-max (.histMax stats)
     :hist-min (.histMin stats)
     :histogram (seq (.histogram stats))
     :histogram-16bit (seq (.histogram16 stats))
     :hist-y-max (.histYMax stats)
     :kurtosis (.kurtosis stats)
     ;:long-histogram (seq (.longHistogram stats))
     :long-pixel-count (.longPixelCount stats)
     ;:lower-threshold (.lowerThreshold stats)
     :major (.major stats)
     :max (.max stats)
     :max-count (.maxCount stats)
     :mean (.mean stats)
     :median (.median stats)
     :min (.min stats)
     :minor (.minor stats)
     :mode (.mode stats)
     :n-bins (.nBins stats)
     :pixel-count (.pixelCount stats)
     :roi-height (.roiHeight stats)
     :roi-width (.roiWidth stats)
     :roi-x (.roiX stats)
     :roi-y (.roiY stats)
     :skewness (.skewness stats)
     :solidity (when (get-roi imp)
                 (/ (.pixelCount stats)
                    (get-polygon-area (.getConvexHull (get-roi imp)))))
     ;:stack-statistics? (.stackStatistics stats)
     :std-dev (.stdDev stats)
     :uncalibrated-mean (.umean stats)
     ;:upper-threshold (.upperThreshold stats)
     ;:width (.width stats)
     :x-center-of-mass (.xCenterOfMass stats)
     :x-centroid (.xCentroid stats)
     :y-center-of-mass (.yCenterOfMass stats)
     :y-centroid (.yCentroid stats)
     ;:pixel-height (.ph stats)
     ;:pixel-width (.pw stats)
     }))   

(defn rolling-ball-statistics
  "Take the statistics of rois within a search radius centered at each input roi."
  [rois search-radius & args]
  (let [argmap (apply hash-map args)
        mergable-keys #{:area :angle :x-center-of-mass :y-center-of-mass :perimeter :circularity :major :minor :mean :median :mode :skewness :std-dev :uncalibrated-mean :x-centroid :y-centroid}
        ; don't forget to keep the ROI
        #_(disj (get-available-measurements)
               :histogram :histogram-16bit)
        distance-keys [:x-center-of-mass :y-center-of-mass]; We don't have to always be spatial
        ]
    (doall 
      (for [target-roi rois]
        (let [neighbors (map #(select-keys % mergable-keys)
                             (filter (fn [nr] 
                                       (let [dist (java.lang.Math/sqrt                                    
                                                    (apply +
                                                           (map #(java.lang.Math/pow (- (target-roi %) (nr %)) 2)
                                                                distance-keys)))]
                                         (< dist search-radius)))
                                     rois))
              merged-maps (apply (partial merge-with +) neighbors)
              ncount (count neighbors)]              
          (assoc (into {}
                       (for [[k v] merged-maps]
                         [k (cond (:average-values argmap)
                                  (if (zero? ncount) 0 (/ v ncount))
                                  :else
                                  v)]))
                 :roi (:roi target-roi)))))))

(defn correlated-rolling-ball-statistics
  "Take the statistics of rois within a search radius centered at each input roi."
  [focal-rois correlation-rois search-radius & args]
  (let [argmap (apply hash-map args)
        mergable-keys #{:area :angle :x-center-of-mass :y-center-of-mass :perimeter :circularity :major :minor :mean :median :mode :skewness :std-dev :uncalibrated-mean :x-centroid :y-centroid}
        ; don't forget to keep the ROI
        #_(disj (get-available-measurements)
               :histogram :histogram-16bit)
        distance-keys [:x-center-of-mass :y-center-of-mass]; We don't have to always be spatial
        ]
    (doall 
      (for [target-roi focal-rois]
        (let [neighbors (map #(select-keys % mergable-keys)
                             (filter (fn [nr] 
                                       (let [dist (java.lang.Math/sqrt                                    
                                                    (apply +
                                                           (map #(java.lang.Math/pow (- (target-roi %) (nr %)) 2)
                                                                distance-keys)))]
                                         (< dist search-radius)))
                                     correlation-rois))
              merged-maps (apply (partial merge-with +) neighbors)
              ncount (count neighbors)]              
          (assoc (if (zero? ncount); might need to do this for normal rolling ball statistics too
                   (into {}
                         (for [k mergable-keys]
                           [k 0]))
                   (into {}
                         (for [[k v] merged-maps]
                           [k (cond (:average-values argmap)
                                    (if (zero? ncount) 0 (/ v ncount))
                                    :else
                                    (if (nil? v) 0 v))])))
                 :roi (:roi target-roi)))))))
                 
            
