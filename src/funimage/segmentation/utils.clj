(ns funimage.segmentation.utils
 (:use [funimage imp project]
       [funimage.imp threshold calculator])
 (:require [clojure.string :as string])
 (:import [mpicbg.imglib.image Image]
          [mpicbg.imglib.cursor Cursor]
          [script.imglib ImgLib]
          [mpicbg.imglib.type.numeric NumericType]
          [ij IJ ImagePlus WindowManager]
          [ij.measure ResultsTable]
          [ij.gui WaitForUserDialog PointRoi NewImage PolygonRoi Roi GenericDialog NonBlockingGenericDialog Line]
          [ij.plugin ImageCalculator Duplicator]
          [ij.plugin.frame RoiManager]
          [ij.process ImageConverter FloatProcessor ByteProcessor]
          [java.awt Button]
          [java.awt.event ActionListener]
          [java.io File]
          #_[weka.clusterers SimpleKMeans]
          #_[weka.core Instances Attribute DenseInstance]))

(defn get-results-table
  "Return the current results table."
  []
  ^ResultsTable (ResultsTable/getResultsTable)) 

