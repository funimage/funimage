(ns funimage.imagej
  (:require [clojure.reflect :as reflect]
            [clojure.pprint :as pprint])
  (:use [funimage img])
  (:import [net.imagej ImageJ]))

(defonce ij (net.imagej.ImageJ.))

(defn open-img
  "Open an image with ImageJ/SCIFIO"
  [filename]
  (.getImg (.getImgPlus (.open (.datasetIO (.scifio ij)) filename))))

(defn show-img
  "Show an image with ImageJ."
  [img]
  (.show (.ui ij) img))

;; Manually configured ops
(defn fft
  "Return a FFT of an Img as an Img."
  [img]     
  (.fft (.filter (.op ij)) img))

(defn convert-img
  "Convert an Img to a different type."
  [new-type img]
  (case new-type
    :float-32 (.float32 (.convert (.op ij)) img)))

(defn create-img-like
  "Create an Img like another Img."
  [img]
  (.img (.create (.op ij)) img))

;; Ops via reflection

;(def tmp (.infos (.op ij)))

#_(def filter-ops 
   (filter #(and (.getName %) (.contains (.getName %) "filter")) tmp)) 

#_(def tmp-op (last filter-ops))

#_(.getName tmp-op)

#_(def fimg (.variance (.filter (.op ij))  img))

;(pprint/print-table (:members (reflect/reflect (class (.op ij)))))

;(def tmp (.help (.op ij)))

#_(do
   #_(require '[clojure.reflect :as r])
   #_(use 'clojure.pprint)
   #_(print-table (:members (r/reflect (class (.convert (.op ij)))))) 
   (def filename "/Volumes/Amnes/SheltonSarah/dA01__m18_df_2015-08-17.tif")
   (def img (open-img filename))
   (show-img img)
   #_(def cimg (convert-img :float-32 img))
   (show-img (convert-img :float-32 (fft img))))
