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

(def output (atom imp))

(def ^ij.plugin.ImageCalculator ic (ij.plugin.ImageCalculator.))

(dotimes [step num-steps]
  (let [^ImagePlus imp2 (.crop @output)
        _  (ij.IJ/run imp2 "Rotate... "
             (str "angle=" angle " grid=1 interpolation=None"))
        ^ImagePlus imp3 (.run ic "OR create" @output imp2)]
    (set! (.changes @output) false)
    (.close @output)
    (ij1/set-title @output (str "Moire-" step))
    (reset! output imp3)))

(ij1/show-imp @output)

; @ImagePlus(label="Target image",description="Input image (will be binarized)") imp
