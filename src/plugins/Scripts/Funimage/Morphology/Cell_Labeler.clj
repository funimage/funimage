; @ImagePlus(label="Target image",description="Input image (expects channel1 is Nuclei channel2 is CellSurface)") input-imp
; @Integer(label="Cell surface channel index",description="Index of the cell surface channel",value=0) cell-surface-channel-index
; @Integer(label="Nuclei channel index",description="Index of the nuclei channel",value=1) nuclei-channel-index
; @Integer(label="Signal channel index",description="Index of the cell surface channel",value=2) signal-channel-index
; @Integer(label="Surface threshold",description="Threshold for creating a mask from cellsurface channel",value=500) surface-threshold
; @Boolean(label="Show all steps",description="Show all processing steps",value=false) show-all-steps
; @OUTPUT ImagePlus output-imp

(use '[funimage img imp project conversion]
     '[funimage.segmentation utils imp]
     '[funimage.imp calibration roi threshold calculator statistics])

; @Parameter(visibility = ItemVisibility.MESSAGE) to write a message

(import '[mpicbg.imglib.image Image]
        '[mpicbg.imglib.cursor Cursor]
        '[script.imglib ImgLib]
        '[mpicbg.imglib.type.numeric NumericType]
        '[mcib3d.image3d.regionGrowing Watershed3D]
        '[ij IJ ImagePlus WindowManager ImageStack]
        '[ij.measure ResultsTable]
        '[ij.gui WaitForUserDialog PointRoi NewImage PolygonRoi Roi GenericDialog NonBlockingGenericDialog Line]
        '[ij.plugin ImageCalculator Duplicator]
        '[ij.plugin.frame RoiManager]
        '[ij.process ImageConverter FloatProcessor ByteProcessor])

(println "Show all steps: " show-all-steps)

(defn dilate-erode-filter
  "Filter by dilate then erode."
  [imp num-dilate num-erode]
  (loop [k num-erode
         imp (loop [k num-dilate
                    imp imp]
               (if (zero? k)
                 imp
                 (recur (dec k)
                        (dilate imp))))]
    (if (zero? k)
      imp
      (recur (dec k)
             (erode imp)))))

(defn split-imp
  "Testing imp splitting."
  [imp]
  (for [k (range (get-num-channels imp))]
    (let [new-stack ^ImageStack (ImageStack. (get-width imp) (get-height imp) #_(get-stack-depth imp))]
      (.setC imp (inc k))
      (dotimes [idx (get-num-slices imp)]
        (.setSlice imp (inc (+ k (* 4 idx))))
        (.addSlice new-stack ^ImageProcessor (.duplicate (.getProcessor imp))))
      (ImagePlus. (str "C" k "-" (.getTitle imp)) new-stack))))

(defn watershed-3d
  "Do a 3D watershed."
  [spot-imp seed-imp voxel-threshold seed-threshold]
  (let [water ^Watershed3D (Watershed3D. (.getStack spot-imp) (.getStack seed-imp) voxel-threshold seed-threshold)
        image-short (.getWatershedImage3D water)]
    (mcib3d.image3d.ImageShort/getImagePlus (.getArray1D image-short) (get-width spot-imp) (get-height spot-imp) (get-num-slices spot-imp) false)
    ))

(defn label-3d
  "Label segments in 3D"
  [imp]
  (let [labeler (mcib3d.image3d.ImageLabeller.)
        minthresh 0]
    (.setMinSize labeler minthresh)
    (.setMaxsize labeler -1)
    (let [img (mcib3d.image3d.ImageInt/wrap imp)
          bin (.thresholdAboveInclusive img minthresh)
          res (.getLabels labeler bin)]      
      #_(println "Num objects:" (.getNbObjectsTotal labeler bin)); there is a count for within size range too, but we count all for now
      (mcib3d.image3d.ImageShort/getImagePlus (.getArray1D res) (get-width imp) (get-height imp) (get-num-slices imp) false))))

(defn relabel-in-8bit
  "Relabel a 16-bit labeled image in 8-bit to start at 1."
  [imp]
  (let [img (imp->img imp)
        idxs (atom {})]
    (map-imgs
      (fn [^Cursor cur]
        (when-not (get @idxs (.getRealFloat (.get cur)))
          (swap! idxs assoc (.getRealFloat (.get cur)) (inc (count @idxs))))        
        (.set (.get cur) (get @idxs (.getRealFloat (.get cur)))))
      img)
    (img->imp img)))

(defn find-centroids-3d
  "Find the 3D centroids of each segment."
  [imp]
  (let [stack ^ImageStack (.getStack imp)
        segs (atom {})]
    (dotimes [z (get-num-slices imp)]
      (dotimes [x (get-width imp)]
        (dotimes [y (get-height imp)]
          (when-not (zero? (.getVoxel stack x y z))
            (swap! segs assoc (.getVoxel stack x y z) (conj (get @segs (.getVoxel stack x y z)) [x y z]))))))
    (for [[k points] @segs]
      (let [sumx (reduce + (map first points))
            sumy (reduce + (map second points))
            sumz (reduce + (map last points))]
        [k [(float (/ sumx (count points)))
            (float (/ sumy (count points)))
            (float (/ sumz (count points)))]]))))

(defn analyze-by-label
  "Analyze an image based on a label image."
  [labels signal]
  (let [label-stack ^ImageStack (.getStack labels)
        signal-stack ^ImageStack (.getStack signal)
        centroids (atom {})
        label-signals (atom {})
        label-counts (atom {})
        signal-sum (atom 0)
        labeled-signal-sum (atom 0)]; just keep track of the total signal to know how much is lost
    (dotimes [z (get-num-slices labels)]
      (dotimes [x (get-width labels)]
        (dotimes [y (get-height labels)]
          (swap! signal-sum #(+ % (.getVoxel signal-stack x y z)))
          (when-not (zero? (.getVoxel label-stack x y z))
            (swap! labeled-signal-sum #(+ % (.getVoxel signal-stack x y z)))
            (swap! centroids assoc (.getVoxel label-stack x y z)
                   (conj (get @centroids (.getVoxel label-stack x y z)) [x y z]))
            (swap! label-signals assoc (.getVoxel label-stack x y z)
                   (conj (get @label-signals (.getVoxel label-stack x y z)) 
                         (.getVoxel signal-stack x y z)))
            (swap! label-counts assoc (.getVoxel label-stack x y z)
                   (if-not (get @label-counts (.getVoxel label-stack x y z))
                     1
                     (inc (get @label-counts (.getVoxel label-stack x y z))))) 
            ))))
    #_(println "Total signal:" @signal-sum)
    #_(println "Total labeled signal:" @labeled-signal-sum)
    (for [k (keys @centroids)]
      (let [points (get @centroids k)
            signal (get @label-signals k)
            counts (get @label-counts k)
            sumx (reduce + (map first points))
            sumy (reduce + (map second points))
            sumz (reduce + (map last points))]
        [k {:x (float (/ sumx (count points)))
            :y (float (/ sumy (count points)))
            :z (float (/ sumz (count points)))
            :total-signal (reduce + signal)
            :avg-signal (float (/ (reduce + signal) counts))
            :count counts}]))))

(defn color-labeled-by-map
   "Color a label image using a map."
   [labels m]
   (let [;imp (create-imp :width (get-width labels) :height (get-height labels) :depth (get-stack-depth labels) :type "32-bit")
         imp (convert-to-float (create-imp-like labels))
         label-stack (.getImageStack labels)
         stack (.getImageStack imp)
         m (assoc m 0 0)]
     (dotimes [z (get-num-slices labels)]
       ;(println (sort (distinct (.getPixels label-stack (inc z)))))
       ;(println (sort (keys m)))
       (.setPixels stack (float-array
                           (map m (.getPixels label-stack (inc z))))
         (inc z))
       #_(dotimes [x (get-width labels)]
          (dotimes [y (get-height labels)]
            (when-not (zero? (.getVoxel label-stack x y z))
              (.setVoxel stack x y z (get m (.getVoxel label-stack x y z)))))))
     imp))


#_(def channels (doall (map #(open-imp (str "/Volumes/Amnes/Claudia/ERG_Dll4_VEcad_IsoB4/07-06-2015/Ctl_P5_IsoB4594_ERG405_Dll4647_VEcad488b_Stitch/" % ".tif")) 
                             ["VEcad" "Dll4" "ERG" "IsoB4"]))) 
  
(def channels (split-channels input-imp))
;(show-imp (first channels))
(def nuclei (blur-gaussian (copy-imp (nth channels nuclei-channel-index)) 5))
(def backup (copy-imp nuclei))
(IJ/run nuclei "Auto Threshold" "method=RenyiEntropy white stack use_stack_histogram"); for the vecad batch  
;(IJ/run nuclei "Auto Threshold" "method=Huang white stack use_stack_histogram"); for the vecad batch  
;(show-imp (autothreshold nuclei :renyi-entropy false false false false false true))
(def clean-nuclei (convert-to-8bit nuclei #_(dilate-erode-filter nuclei 0 3)))
(imp-multiply clean-nuclei (invert (nth channels nuclei-channel-index)))
(.setTitle clean-nuclei "CleanNuclei")
;(copy-calibration imp clean-nuclei)
(copy-calibration (first channels) clean-nuclei)
(when show-all-steps
  (show-imp clean-nuclei))

;(IJ/run "3D Watershed" (str "seeds_threshold=25 image_threshold=25 image=" (.getTitle clean-nuclei) " seeds=" (.getTitle clean-nuclei) " radius=2"))
;(def labeled-nuclei (label-3d (watershed-3d clean-nuclei clean-nuclei 25 25)))
(def labeled-nuclei (watershed-3d clean-nuclei clean-nuclei 25 25))
(.setTitle labeled-nuclei "LabeledNuclei")
;(copy-calibration imp labeled-nuclei)
(copy-calibration (first channels) labeled-nuclei)
(when show-all-steps
  (show-imp labeled-nuclei))  
(def all-centroids (find-centroids-3d labeled-nuclei))
#_(doseq [[k centroid] all-centroids]
   (println "Segment" k ": " centroid))

(.close clean-nuclei)
;(def clean-nuclei ((comp erode erode erode) (convert-to-8bit (threshold-stack labeled-nuclei 1)))) ;(show-imp clean-nuclei)
;(def clean-nuclei (convert-to-8bit (threshold-stack labeled-nuclei 1))) ;(show-imp clean-nuclei)
;(def clean-nuclei (threshold-stack (convert-to-8bit (threshold-stack labeled-nuclei 1)) 1)) ;(show-imp clean-nuclei)
(def clean-nuclei (threshold-stack labeled-nuclei 1)) ;(show-imp clean-nuclei)
;(IJ/run clean-nuclei "Erode (3D)" "iso=1")
(.setTitle clean-nuclei "CleanNuclei")
(when show-all-steps
  (show-imp clean-nuclei))

; Split nuclei on size

;(def boundaries (watershed-3d (second channels) clean-nuclei 10000 25))
;(def boundaries (watershed-3d (first channels) clean-nuclei 10000 25))
;(def boundaries (watershed-3d (first channels) clean-nuclei 10000 25))
(def boundaries (watershed-3d (nth channels cell-surface-channel-index) clean-nuclei surface-threshold 1))
;(def boundaries (watershed-3d (nth channels 2) clean-nuclei 10000 25))
(.setTitle boundaries "Boundaries")
;(copy-calibration imp boundaries)
(copy-calibration (first channels) boundaries)
(when show-all-steps
  (show-imp boundaries))

(def mask (copy-imp (nth channels cell-surface-channel-index)))
(copy-calibration (first channels) mask)
(convert-to-8bit (threshold-stack mask surface-threshold))
(.setTitle mask "Mask")
(when show-all-steps
  (show-imp mask))

#_(def cell-shape (watershed-3d (imp-subtract (copy-imp mask) 
                                             (threshold-stack (copy-imp (first channels)) 9252)) clean-nuclei 25 25))
(def cell-shape (watershed-3d (copy-imp mask) 
                              clean-nuclei 25 25))
(.setTitle cell-shape "CellShape")
(copy-calibration (first channels) cell-shape)
(when show-all-steps
  (show-imp cell-shape))
;(IJ/run cell-shape "16 ramps" "")

;(.setMinAndMax cell-shape 0 128)
#_(def merged-labels (imp-subtract (copy-imp cell-shape)
                                  clean-nuclei))
;(show-imp merged-labels)

#_(def cell-results (analyze-by-label cell-shape (nth channels signal-channel-index)))

#_(when show-all-steps
   (doseq [[k v] cell-results]
     (println k v))
   (print (sort (map (comp :avg-signal second) cell-results))))

;(def dll4-cells (color-labeled-by-map cell-shape (zipmap (keys cell-results) (map (comp :avg-signal second) cell-results))))
#_(def dll4-cells (color-labeled-by-map cell-shape (zipmap (map (comp int first) cell-results) (map (comp :avg-signal second) cell-results))))
#_(.setTitle dll4-cells "Dll4Labeling")
#_(when show-all-steps
   (show-imp dll4-cells))

(def output-imp (autocontrast (zconcat-imps (concat channels [cell-shape]))))
