(ns funimage.video
  (:use [funimage imp]))        

; This expects that you've made the calls to activate FFMPEG via FIJI
; Super hard-coded for now

(defn save-z-as-avi
  "Save a Z-stack as an avi."
  [imp filename]
  (ij.IJ/run imp "AVI... " (str "compression=JPEG frame=7 save=" filename )))

(defn open-tga-directory
  "Open a directory of tga files as an imagestack."
  [directory]
  (let [listing (.listFiles (java.io.File. directory))]
    (zconcat-imps
      (for [file listing]
        (let [imp (open-imp (.getAbsolutePath file))]
          (ij.IJ/run imp "Hyperstack to Stack" "")
          (let [conv ^ij.process.ImageConverter. (ij.process.ImageConverter. imp)]            
            (.convertRGBStackToRGB conv)
            imp))))))

(defn tga-sequence-to-avi
  "Take a TGA (RAW) sequence as a directory, and make an avi."
  [directory avi-filename]
  (let [imp (open-tga-directory directory)]
    (save-z-as-avi imp avi-filename)))

