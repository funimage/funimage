(def dialog (ij.gui.NonBlockingGenericDialog. "Nuclei Center Labeler (3D)"))
(def add-point-button (java.awt.Button. "Add point"))
(def save-button (java.awt.Button. "Save points"))
(def rtable (ij.measure.ResultsTable/getResultsTable))

(defn add-point []
  (let [roi (.getRoi (ij.IJ/getImage))
  	bounds (.getBounds roi)
  	idx (.getCounter rtable)]
    (.setValue rtable "X" idx (double (.x bounds)))
    (.setValue rtable "Y" idx (double (.y bounds)))
    (.setValue rtable "Z" idx (double (.getZ (ij.IJ/getImage))))
    (.updateResults rtable)
    (.show rtable "Results")))

(.add dialog add-point-button)
(.add dialog save-button)
(.addActionListener add-point-button 
	(reify java.awt.event.ActionListener
		(actionPerformed [_ evt]
			(add-point))))
(.addActionListener save-button 
	(reify java.awt.event.ActionListener
		(actionPerformed [_ evt]
		  (let [filename (.getTitle (ij.IJ/getImage))
		  	directory (str (.directory (.getOriginalFileInfo (ij.IJ/getImage))))
		  	outname (str directory #_java.io.File/separator 
					(.substring filename 0 (.lastIndexOf filename "."))
				     "_NucleiCenters.csv")]
		    (ij.IJ/log (str "Saving results to " outname))
			(.saveAs rtable outname)))))
(.addMessage dialog " ")
(.showDialog dialog)


(ij.IJ/setTool "point")


