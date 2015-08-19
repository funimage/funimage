(ns funimage.imp.calibration
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
           [ij.measure Calibration]
           
           [ij.io Opener]
           ;[io.scif.img ImgOpener]
           
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

(defn get-x-unit
  "Return the units of the x-axis."
  [^Calibration cal]
  (.getXUnit cal))

(defn get-y-unit
  "Return the units of the y-axis."
  [^Calibration cal]
  (.getYUnit cal))

(defn get-z-unit
  "Return the units of the z-axis."
  [^Calibration cal]
  (.getZUnit cal))

(defn x-pixel->physical
  "Convert an x-value in pixel coordinates to physical coordinates."
  [^Calibration cal ^double x]
  (.getX cal x))

(defn y-pixel->physical
  "Convert an y-value in pixel coordinates to physical coordinates."
  [^Calibration cal ^double y]
  (.getY cal y))

(defn z-pixel->physical
  "Convert an z-value in pixel coordinates to physical coordinates."
  [^Calibration cal ^double z]
  (.getZ cal z))

(defn pixel-width
  "Return the physical dimensions of a single pixel along the x-axis."
  [^Calibration cal]
  (.pixelWidth cal))

(defn pixel-height
  "Return the physical dimensions of a single pixel along the y-axis."
  [^Calibration cal]
  (.pixelHeight cal))

(defn pixel-depth
  "Return the physical dimensions of a single pixel along the z-axis."
  [^Calibration cal]
  (.pixelDepth cal))

  
