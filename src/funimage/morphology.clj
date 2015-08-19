(ns funimage.morphology
  (:require [clojure.string :as string])
  (:import [java.io File]
           [ij IJ]           
           [loci.plugins BF]
           [ij.io FileSaver]
           [javax.media.j3d Transform3D]
           [javax.vecmath Vector3f Point3f Quat4f]
           [ij.gui NewImage]
           [ij.process ImageProcessor ByteProcessor ImageStatistics ImageConverter] 
           
           [java.util.concurrent Executors]
           
           [net.imglib2.img ImagePlusAdapter Img]
           [net.imglib2.img.display.imagej ImageJFunctions]
           [net.imglib2.type NativeType]
           [net.imglib2.type.numeric NumericType]
           [net.imglib2.type.numeric.real FloatType]
           [net.imglib2.algorithm.gauss3 Gauss3]
           [net.imglib2.algorithm.dog DifferenceOfGaussian]
           [net.imglib2.view Views]
           [net.imglib2 Cursor]
           
           [net.imglib2.ops.types ConnectedType]
           [net.imglib2.algorithm.region.localneighborhood Shape HyperSphereShape RectangleShape]
           
           [net.imglib2.converter ARGBARGBDoubleConverter]
           )
  (:use [funimage.basic]))

(def four-neighbors-connectedtype ConnectedType/FOUR_CONNECTED)
(def eight-neighbors-connectedtype ConnectedType/EIGHT_CONNECTED)

(defn hyper-sphere-shape
  "Return a structural element of a hyper sphere."
  [radius]
  (HyperSphereShape. radius))

(defn rectangle-shape
  "Return a structural element of a rectangle."
  [span skip-center]; for cuboids
  (RectangleShape. span skip-center))
