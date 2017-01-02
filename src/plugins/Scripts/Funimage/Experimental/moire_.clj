; @Integer(label="Num steps") num-steps
; @Float(label="Angle (degree)") angle
; @OUTPUT ImagePlus imp

(require '[funimage.imp :as ij1])

(def imp (ij.IJ/createImage "Moire" "8-bit black" 500 500 1))
;(ij.IJ/run imp "Add Noise" "")
(ij.IJ/run imp "Salt and Pepper" "")

(ij.IJ/run imp "8-bit" "")
(ij.IJ/setAutoThreshold imp, "Default dark")
(ij.IJ/run imp "Convert to Mask" "")

(def ^ij.plugin.ImageCalculator ic (ij.plugin.ImageCalculator.))

(def output
  (atom
   (ij1/zconcat-imps
    (for [step (range num-steps)]
       (let [^ij.ImagePlus imp2 (.crop imp)]
         (println step (* step angle))
         (ij.IJ/run imp2 "Rotate... "
           (str "angle=" (* step angle) " grid=1 interpolation=None"))
         ^ij.ImagePlus (.run ic "OR create" imp imp2))))))

#_(dotimes [step num-steps]
  (let [^ij.ImagePlus imp2 (.crop imp)
        _  (ij.IJ/run imp2 "Rotate... "
             (str "angle=" angle " grid=1 interpolation=None"))
        ^ij.ImagePlus imp3 (.run ic "OR create" imp imp2)]
    (.addSlice (.getImageStack @output) ^ij.process.ImageProcessor (.getProcessor imp3))
    (ij1/set-title @output "Moire")))

(ij1/show-imp @output)

; @ImagePlus(label="Target image",description="Input image (will be binarized)") imp
