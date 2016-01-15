(ns funimage.skeletonize
  (:require [clojure.string :as string]
            [clojure.java.io :as io])
  (:import [java.io File]
           [ij IJ ImagePlus ImageStack]
           [ij.io FileSaver]
           [ij.gui NewImage Toolbar]
           [ij.process ImageProcessor ByteProcessor ImageStatistics])           
  (:use [funimage imp]))

(defn skeletonize-2d
  "Skeletonize a 2D binary image."
  [imp]
  (let [plugin (ij.plugin.filter.Binary.)]
    (.setup plugin "skeletonize" imp)
    (.skeletonize plugin)))
        

