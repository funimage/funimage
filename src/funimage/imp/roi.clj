(ns funimage.imp.roi;; This is the namespace for ImagePlus utilities (see img for imglib2 img's)
  (:use [funimage imp])
  (:require [clojure.string :as string]
            [clojure.java.io :as io])
  (:import [java.io File]
           [ij IJ ImagePlus ImageStack]
           [loci.plugins BF]
           [ij.io FileSaver]
           [javax.media.j3d Transform3D]
           [javax.vecmath Vector3f Point3f Quat4f]
           [ij.gui NewImage Toolbar Roi OvalRoi]
           [ij.process ImageProcessor ByteProcessor ImageStatistics]
           [ij.measure Calibration]
           [ij.plugin.frame RoiManager]
           
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

(defn rectangle-roi
  "Make a rectangular ROI."
  [x y w h]
  ^Roi (Roi. x y w h))

(defn oval-roi
  "Make an oval ROI."
   [x y w h]
   ^Roi (OvalRoi. x y w h))

(defn get-roi-manager
  "Get the current ROI manager."
  []
  (let [rm ^RoiManager (RoiManager/getInstance)]
    (if rm
      rm
      ^RoiManager (RoiManager.))))

(defn get-rois
  "Get all ROIs."
  []
  (let [roi-manager (get-roi-manager)]
     (.getRoisAsArray roi-manager)))

(defn mask-from-roi-manager
  "Make a mask ImagePlus from all the rois in roi-manager."
  [roi-manager imp]
  (let [mask (create-imp-like imp)]    
    (.setSnapshotCopyMode (.getProcessor mask) false)
    (set-fill-value mask 255)
    (doseq [roi (.getRoisAsArray roi-manager)]
      #_(set-roi mask roi)
      (.fillPolygon ^ij.process.ImageProcessor (.getProcessor mask) ^java.awt.Polygon (.getPolygon roi)))
    mask))

(defn color-mask-rois
  "Make a colored mask of ROIs, expects: [[roi1 color1], [roi2 color2], ...]"
  [imp rois colors]
  (let [mask (create-imp-like imp)]
    (.setSnapshotCopyMode (.getProcessor mask) false)
    (dotimes [k (count rois)]      
      (set-roi mask (nth rois k))      
      (set-fill-value mask (nth colors k))
      (.fillPolygon ^ij.process.ImageProcessor (.getProcessor mask) ^java.awt.Polygon (.getPolygon (nth rois k))))
    mask))

#_(let [filename "hello-communist-kitty_bw.tif"
       imp (open-imp filename)
       patch-width 50 patch-height 50
       w (get-width imp) h (get-height imp)
       rois (for [x (range 0 w patch-width)
                  y (range 0 w patch-height)]
              (rectangle-roi x y patch-width patch-height))
       colors (repeatedly (count rois) #(rand-int 255))
       patched-imp (color-mask-rois imp rois colors)]
   (show-imp imp)
   (show-imp patched-imp))
  