(ns funimage.imagej
  (:import [net.imagej ImageJ])

(defonce ij (net.imagej.ImageJ.))

;(def filename "/Volumes/Amnes/SheltonSarah/dA01__m18_df_2015-08-17.tif")
;(def dataset (.open (.datasetIO (.scifio ij)) filename))˘
;(.show (.ui ij) dataset)

(defn open-img
  "Open an image with ImageJ/SCIFIO"
  [filename]
  (.getImg (.getImgPlus (.open (.datasetIO (.scifio ij)) filename))))

(defn show-img
  "Show an image with ImageJ."
  [img]
  (.show (.ui ij) img))

;(defn fft
;  "Return a FFT of an Img as an Img."
;  [img] 

#_(let [filename "/Volumes/Amnes/SheltonSarah/dA01__m18_df_2015-08-17.tif"]
   (show-img (open-img filename)))
