; This is the namespace for converting datatypes (i.e. ImagePlus -> Img and vice versa)
(ns funimage.conversion
  (:require [clojure.string :as string]
            [clojure.java.io :as io])
  (:import [java.io File]
           [ij IJ ImagePlus]
           [loci.plugins BF]
           [ij.io FileSaver]
           [javax.media.j3d Transform3D]
           [javax.vecmath Vector3f Point3f Quat4f]
           [ij.gui NewImage Toolbar]
           [ij.process ImageProcessor ByteProcessor ImageStatistics]
           
           [ij.io Opener]
           
           [java.util.concurrent Executors]
           [java.awt Canvas Graphics]
           [javax.swing JFrame JMenu JMenuBar JMenuItem]
           
           [net.imglib2.img ImagePlusAdapter Img]
           [net.imglib2.img.display.imagej ImageJFunctions]
           [net.imglib2.type NativeType]
           [net.imglib2.type.numeric NumericType ARGBType]
           [net.imglib2.type.numeric.real FloatType]
           [net.imglib2.algorithm.gauss3 Gauss3]
           [net.imglib2.algorithm.dog DifferenceOfGaussian]
           [net.imglib2.view Views IntervalView]
           [net.imglib2 Cursor RandomAccess]))

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
