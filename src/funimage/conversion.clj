; This is the namespace for converting datatypes (i.e. ImagePlus -> Img and vice versa)
(ns funimage.conversion
  (:require [clojure.string :as string]
            [clojure.java.io :as io])
  (:import [java.io File]
           [ij IJ ImagePlus]
           
           #_[ij.io Opener]
                      
           [net.imglib2.img ImagePlusAdapter Img]
           [net.imglib2.img.display.imagej ImageJFunctions]
           #_[net.imglib2.type NativeType]
           #_[net.imglib2.view Views IntervalView]
           #_[net.imglib2 Cursor RandomAccess]))

(defn imp->img
  "Convert and ImagePlus to an Img"
  [imp]
  (ImagePlusAdapter/wrap ^ImagePlus imp))

(defn img->imp
  "Convert an Img to an ImagePlus"
  [img]
  (ImageJFunctions/wrap ^Img img "Wrapped Img"))

(defn imp->awt-image
  "Return a java.awt.Image from this imageplus."
  [imp]
  (.createImage ^ImagePlus imp))

(defn imp->buffered-image
  "Return a BufferedImage from an ImagePlus."
  [^ImagePlus imp]
  (.getBufferedImage imp))
