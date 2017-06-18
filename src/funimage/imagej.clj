(ns funimage.imagej
  (:require [clojure.pprint :as pprint]
            [clojure.string :as string]
            [funimage.img :as img])
  (:import [net.imagej ImageJ]))

(let [context (org.scijava.Context.) 
      #_(org.scijava.Context. [org.scijava.thread.ThreadService
                              org.scijava.log.LogService
                              org.scijava.io.IOService
                              org.scijava.display.DisplayService
                              org.scijava.convert.ConvertService
                              org.scijava.command.CommandService
                              org.scijava.input.InputService
                              org.scijava.script.ScriptService
                              org.scijava.app.StatusService
                              net.imagej.ops.OpService
                              io.scif.services.SCIFIODatasetService
                              io.scif.xml.XMLService
                              io.scif.services.InitializeService
                              io.scif.img.converters.PlaneConverterService
                              io.scif.img.ImgUtilityService
                              io.scif.services.DatasetIOService])
      new-ij (net.imagej.ImageJ. context)]
  (defonce ij new-ij))

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
