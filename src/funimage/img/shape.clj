(ns funimage.img.shape
  (:import [net.imglib2.algorithm.neighborhood Neighborhood RectangleShape]
           [net.imglib2.util Intervals]
           [net.imglib2.img ImagePlusAdapter Img]
           [net.imglib2.img.display.imagej ImageJFunctions]
           [net.imglib2.type NativeType]
           [net.imglib2.type.numeric NumericType ARGBType]
           [net.imglib2.type.numeric.real FloatType]
           [net.imglib2.view Views IntervalView]
           [net.imglib2 Cursor RandomAccess RandomAccessibleInterval Interval])
  (:require [funimage.img :as img]
            [funimage.img.cursor :as cursor]
            [funimage.imagej.ops :as ops]))

(defn rectangle-shape
  "Return a RectangleShape."
  [length]
  (net.imglib2.algorithm.neighborhood.RectangleShape. length true))

(defn sphere-shape
  "Return a sphere shape"
  [radius]
  (net.imglib2.algorithm.neighborhood.HyperSphereShape. radius))
