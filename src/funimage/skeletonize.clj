(ns funimage.skeletonize
  (:require [clojure.string :as string]
            [clojure.java.io :as io])
  (:import [java.io File]
           [ij IJ ImagePlus ImageStack]
           [ij.io FileSaver]
           [javax.media.j3d Transform3D]
           [javax.vecmath Vector3f Point3f Quat4f]
           [ij.gui NewImage Toolbar]
           [ij.process ImageProcessor ByteProcessor ImageStatistics])           
  (:use [funimage imp]))

#_(defn skeletonize-3d
   "Skeletonize an imageplus"
   [imp]
   (let [inst (AnalyzeSkeleton_.)]
     (.setup inst "" imp)
     (.run inst (.getProcessor imp))))

(defn skeletonize-2d
  "Skeletonize a 2D binary image."
  [imp]
  (let [plugin (ij.plugin.filter.Binary.)]
    (.setup plugin "skeletonize" imp)
    (.skeletonize plugin))); ^ij.process.ImageProcessor (.getProcessor imp))))
        

#_(do (require '[clojure.reflect :as r])
   (use 'clojure.pprint)
   (print-table (:members (r/reflect ij.plugin.filter.Binary))))

