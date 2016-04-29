; @ImagePlus(label="Target image",description="Input image") imp
; @Integer(label="Smoothing steps",description="Number of steps of smoothing",value=0) smoothing-steps
; @OUTPUT ImagePlus imp

(def orig-name (.getTitle imp))
(def directory (ij.IJ/getDirectory "image"))

; Requires the plugin from:
; morphology update site 
; and
; http://jvsmicroscope.uta.fi/?q=skeleton_intersections

(ij.IJ/run imp "Subtract Background..." "rolling=25")
(def channels (ij.plugin.ChannelSplitter/split imp))

(def mask (second channels))
(.setTitle mask (str "C2-" orig-name ))

(dotimes [step smoothing-steps]
  (ij.IJ/run mask "Smooth" ""))

(ij.IJ/run mask "Auto Threshold" "method=IsoData white")

(ij.IJ/run mask "Skeletonize (2D/3D)" "")

(ij.IJ/run mask "Analyze Particles..." "size=1000-Infinity pixel show=Masks display clear add")

(def new-mask (ij.WindowManager/getImage (str "Mask of " (str "C2-" orig-name ))))

(ij.IJ/run new-mask "Invert LUT" "")

;(.setTitle new-mask orig-name)

(ij.IJ/run new-mask "Skeleton Intersections" "pseudo results")

(let [ic (ij.plugin.ImageCalculator.)
	  intersections (ij.WindowManager/getImage "Skeleton intersections")]
	(.run ic "add" imp intersections))

(set! (.changes new-mask) false)
(.close new-mask)

;(println "saving as" (str (string/replace (string/replace orig-name ".tif" "") " " "_") "_labeled_branches.tif"))
;(ij.IJ/saveAsTiff imp (str directory "/" (string/replace orig-name ".tif" "") "_labeled_branches.tif"))

;(ij.IJ/run "Close All")