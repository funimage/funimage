(ns funimage.imp.misc
  (:use [funimage imp]
        [funimage.imp calculator])
  (:require [clojure.string :as string]
            [clojure.java.io :as io]  )

(defn subtract-background-rolling-ball-2D
  "Perform 2D rolling ball subtraction."
  [^ij.ImagePlus imp radius]
  (IJ/run imp "Subtract Background..." (str "rolling=" radius)))


