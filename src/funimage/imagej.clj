(ns funimage.imagej
  (:require [clojure.reflect :as reflect]
            [clojure.pprint :as pprint]
            [clojure.string :as string])
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

(defn show-ui
  "Show the ImageJ UI"
  []
  (.showUI (.ui ij)))
