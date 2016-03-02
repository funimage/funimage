; @ImagePlus(label="Target image",description="Input image") imp

(import 'ij.IJ)
(import 'ij.plugin.frame.RoiManager)

(def imp (ij.IJ/getImage))

(ij.IJ/setTool "point")

(def dialog (ij.gui.NonBlockingGenericDialog. "Cube labeler (3D)"))
;(def add-point-button (java.awt.Button. "Add cube"))
(def add-fg-button (java.awt.Button. "Add foreground cube"))
(def add-bg-button (java.awt.Button. "Add background cube"))
(def set-p1-button (java.awt.Button. "Set cube point1"))
(def set-p2-button (java.awt.Button. "Set cube point2"))
(def save-button (java.awt.Button. "Save cubes"))
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
    (.setValue rtable "Z1" idx (double (.getZ imp)))
    (.updateResults rtable)
    (.show rtable "Results")))

(defn set-second-point []
  (let [roi (.getRoi imp)
        bounds (.getBounds roi)
        idx (dec (.getCounter rtable))]
    (.setValue rtable "X2" idx (double (.x bounds)))
    (.setValue rtable "Y2" idx (double (.y bounds)))
    (.setValue rtable "Z2" idx (double (.getZ imp)))
    (.updateResults rtable)
    (.show rtable "Results")))


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

(.addMessage dialog " ")
(.showDialog dialog)
