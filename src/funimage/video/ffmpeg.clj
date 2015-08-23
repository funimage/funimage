(ns funimage.video.ffmpeg
  (:use [funimage imp]))        

; This expects that you've made the calls to activate FFMPEG via FIJI
; Super hard-coded for now

(defn save-z-as-avi
  "Save a Z-stack as an avi."
  [imp filename]
  (ij.IJ/run imp "AVI... " (str "compression=JPEG frame=7 save=" filename )))
