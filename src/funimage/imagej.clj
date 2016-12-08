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

(defn show
  "Show an image with ImageJ."
  [img]
  (.show (.ui ij) img))

(defn show-ui
  "Show the ImageJ UI"
  []
  (.showUI (.ui ij)))
