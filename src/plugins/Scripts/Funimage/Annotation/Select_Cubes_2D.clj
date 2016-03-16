; @ImagePlus(label="Target image",description="Input image") imp

(import 'ij.IJ)
(import 'ij.plugin.frame.RoiManager)
(use 'funimage.imp)
(require '[clojure.string :as string])

(def imp (ij.IJ/getImage))

;(ij.IJ/setTool "point")
; rect
(ij.IJ/setTool "rect")

(def dialog (ij.gui.NonBlockingGenericDialog. "Cube labeler (3D)"))
;(def add-point-button (java.awt.Button. "Add cube"))
(def add-fg-button (java.awt.Button. "Add foreground cube"))
(def add-bg-button (java.awt.Button. "Add background cube"))
(def set-p1-button (java.awt.Button. "Set cube top"))
(def set-p2-button (java.awt.Button. "Set cube bottom"))
(def save-button (java.awt.Button. "Save cubes"))
(def load-button (java.awt.Button. "Load cubes"))
(def visualize-button (java.awt.Button. "Visualize Mask"))
(def rtable (ij.measure.ResultsTable/getResultsTable))

#_(defn add-point []
    (let [roi (.getRoi (ij.IJ/getImage))
  	  bounds (.getBounds roi)
  	  idx (.getCounter rtable)]
      (.setValue rtable "X" idx (double (.x bounds)))
      (.setValue rtable "Y" idx (double (.y bounds)))
      (.setValue rtable "Z" idx (double (.getZ (ij.IJ/getImage))))
      (.updateResults rtable)
      (.show rtable "Results")))

(defn add-cube [background?]
    (let [idx (.getCounter rtable)]
      (.setValue rtable "BG" idx (double (if background? 1 0)))
      (.setValue rtable "X1" idx -1.0)
      (.setValue rtable "Y1" idx -1.0)
      (.setValue rtable "Z1" idx -1.0)
      (.setValue rtable "X2" idx -1.0)
      (.setValue rtable "Y2" idx -1.0)
      (.setValue rtable "Z2" idx -1.0)
      (.updateResults rtable)
      (.show rtable "Results")))

(defn set-first-point []
  (let [roi (.getRoi imp)
        bounds (.getBounds roi)
        idx (dec (.getCounter rtable))]
    (.setValue rtable "X1" idx (double (.x bounds)))
    (.setValue rtable "Y1" idx (double (.y bounds)))
    (.setValue rtable "X2" idx (double (+ (.x bounds) (.width bounds))))
    (.setValue rtable "Y2" idx (double (+ (.y bounds) (.height bounds))))
    (.setValue rtable "Z1" idx (double (.getZ imp)))
    (.updateResults rtable)
    (.show rtable "Results")))

(defn set-second-point []
  (let [roi (.getRoi imp)
        bounds (.getBounds roi)
        idx (dec (.getCounter rtable))]
    (.setValue rtable "X1" idx (double (.x bounds)))
    (.setValue rtable "Y1" idx (double (.y bounds)))
    (.setValue rtable "X2" idx (double (+ (.x bounds) (.width bounds))))
    (.setValue rtable "Y2" idx (double (+ (.y bounds) (.height bounds))))
    (.setValue rtable "Z2" idx (double (.getZ imp)))
    (.updateResults rtable)
    (.show rtable "Results")))

(defn get-cube
  "Return the cube at the specified index."
  [idx]
  (when (< idx (.getCounter rtable))
    (let [row-string (.getRowAsString rtable idx)
          parts (map read-string (string/split row-string #"\t"))
          [uid bg x1 y1 z1 x2 y2 z2] parts]
      (println "get-cube: " idx parts)
      {:background? (not (zero? bg))
       :x1 x1
       :y1 y1
       :z1 z1
       :x2 x2
       :y2 y2
       :z2 z2})))

(defn fill-cube
  "Fill a cube in the given image."
  [cube tofill fill-value]
  (when (reduce #(and %1 %2)
                (map (comp #(not (neg? %))
                           #(cube %))
                     [:x1 :y1 :z1 :x2 :y2 :z2]))
    (println "filling x: " (min (:x1 cube) (:x2 cube)) " " (max (:x1 cube) (:x2 cube)))
    (println "filling y: " (min (:y1 cube) (:y2 cube)) " " (max (:y1 cube) (:y2 cube)))
    (println "filling z: " (min (:z1 cube) (:z2 cube)) " " (max (:z1 cube) (:z2 cube)))
    (doall
      (for [x (range (min (:x1 cube) (:x2 cube))
                     (max (:x1 cube) (:x2 cube)))
            y (range (min (:y1 cube) (:y2 cube))
                     (max (:y1 cube) (:y2 cube)))
            z (range (min (:z1 cube) (:z2 cube))
                     (max (:z1 cube) (:z2 cube)))]
        (set-voxel tofill x y z fill-value)))))

(defn imps-to-rgb3
  "Convert a sequence of imps (only first 3 or fewer if less supplied) to RGB."
  [imps & args]
  (ij.IJ/run "Merge Channels..."
             (str "c1=" (get-title (first imps))
                  " "
                  "c2=" (get-title (second imps))
                  " "
                  "c3=" (get-title (last imps))
                  #_" keep"))
  #_(ij.plugin.RGBStackMerge/mergeHyperstacks (into-array ij.ImagePlus (take 3 imps)) false)
  #_(let [argmap (apply hash-map args)]
     (.mergeHyperstacks (ij.plugin.RGBStackMerge.) (into-array ij.ImagePlus (take 3 imps)) (or (:keep-source argmap) false))))

(defn visualize-mask
  "Use the results table and the current image to create a RGB image of source/background/foreground"
  []
  (let [foreground-imp (set-title (create-imp-like imp) (str "FG-" (get-title imp)))
        background-imp (set-title (create-imp-like imp) (str "BG-" (get-title imp)))]
    ;; Fill in the foreground and background imps    
    (println "Headings " (map #(str %) (.getHeadings rtable)))
    (dotimes [k (.getCounter rtable)]
      (let [cube (get-cube k)]
        (fill-cube cube
                   (if (:background? cube) background-imp foreground-imp)
                   35535)));; Assume 16-bit for now
    (show-imp foreground-imp)
    (show-imp background-imp)    
    (show-imp (set-title (imps-to-rgb3 (map (comp convert-to-8bit autocontrast)
                                            [foreground-imp (show-imp (copy-imp imp)) background-imp]))
                        "Mask Visualization"))))

;; Dialog construction

;(.add dialog add-point-button)
#_(.addActionListener add-point-button 
	 (reify java.awt.event.ActionListener
		 (actionPerformed [_ evt]
       (add-cube)
			 #_(add-point))))

(.add dialog add-fg-button)
(.addActionListener add-fg-button 
	(reify java.awt.event.ActionListener
		(actionPerformed [_ evt]
      (add-cube false))))

(.add dialog add-bg-button)
(.addActionListener add-bg-button 
	(reify java.awt.event.ActionListener
		(actionPerformed [_ evt]
      (add-cube true))))

(.add dialog set-p1-button)
(.addActionListener set-p1-button 
	(reify java.awt.event.ActionListener
		(actionPerformed [_ evt]
      (set-first-point))))

(.add dialog set-p2-button)
(.addActionListener set-p2-button 
	(reify java.awt.event.ActionListener
		(actionPerformed [_ evt]
      (set-second-point))))

(.add dialog visualize-button)
(.addActionListener visualize-button 
	(reify java.awt.event.ActionListener
		(actionPerformed [_ evt]
      (visualize-mask))))

(.add dialog save-button)
(.addActionListener save-button 
	(reify java.awt.event.ActionListener
		(actionPerformed [_ evt]
		  (let [filename (.getTitle imp)
          directory (str (.directory (.getOriginalFileInfo imp)))
          outname (str directory java.io.File/separator 
                       (.substring filename 0 (.lastIndexOf filename ".")) 
                       "_CubeROIs.csv")]
		    (ij.IJ/log (str "Saving results to " outname))
			(.saveAs rtable outname)))))

(.add dialog load-button)
(.addActionListener load-button 
	(reify java.awt.event.ActionListener
		(actionPerformed [_ evt]
      (let [open-dialog (ij.io.OpenDialog. "Choose the cube file.")
            filename (str (.getDirectory open-dialog) java.io.File/separator (.getFileName open-dialog))]
        (doseq [line (rest (string/split-lines (slurp filename)))]
          (let [idx (.getCounter rtable)
                parts (into [] (rest (string/split line #",")))
                [bg x1 y1 z1 x2 y2 z2] parts]
            (.setValue rtable "BG" idx bg)
            (.setValue rtable "X1" idx x1)
            (.setValue rtable "Y1" idx y1)
            (.setValue rtable "Z1" idx z1)
            (.setValue rtable "X2" idx x2)
            (.setValue rtable "Y2" idx y2)
            (.setValue rtable "Z2" idx z2)
            (.updateResults rtable)))
        (.show rtable "Results")))))

(.addMessage dialog " ")
(.showDialog dialog)
