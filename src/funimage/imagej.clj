(ns funimage.imagej
  (:require [clojure.pprint :as pprint]
            [clojure.string :as string]
            [funimage.img :as img])
  (:import [net.imagej ImageJ]))

(defonce ij (net.imagej.ImageJ.))

(defn open-img
  "Open an image with ImageJ/SCIFIO"
  [filename]
  (.getImg (.getImgPlus (.open (.datasetIO (.scifio ij)) filename))))

(defn save-img
  "Open an image with ImageJ/SCIFIO"
  [img filename]
  (.save (.datasetIO (.scifio ij))
    (net.imagej.DefaultDataset. 
      (.context ij) (net.imagej.ImgPlus. img)) 
    filename))

(defn show
  "Show an image with ImageJ."
  ([img]
    (.show (.ui ij) img))
  ([img title]
    (.show (.ui ij) title img)))

(defn notebook-show
  [image]
  (.display (.notebook ij) image))

(defn show-ui
  "Show the ImageJ UI"
  []
  (.showUI (.ui ij)))
