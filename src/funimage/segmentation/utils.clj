(ns funimage.segmentation.utils
 (:use [funimage imp project]
       [funimage.imp threshold calculator])
 (:require [clojure.string :as string])
 (:import [ij IJ ImagePlus WindowManager]
          [ij.measure ResultsTable]
          [ij.gui WaitForUserDialog PointRoi NewImage PolygonRoi Roi GenericDialog NonBlockingGenericDialog Line]
          [ij.plugin ImageCalculator Duplicator]
          [ij.plugin.frame RoiManager]
          [ij.process ImageConverter FloatProcessor ByteProcessor]
          [java.awt Button]
          [java.awt.event ActionListener]
          [java.io File]))

(defn get-results-table
  "Return the current results table."
  []
  ^ResultsTable (ResultsTable/getResultsTable)) 

(defn results-table-to-map
  "Convert a results table into a hash map."
  [rt]
  (let [;rt (get-results-table)
        headings (.getHeadings rt)]
    (apply merge
           (doall (for [k (range (.getCounter rt))]
                    {k 
                     (apply hash-map
                            (flatten (for [heading headings]
                                       [(keyword heading) (.getValue rt heading k)])))})))))



