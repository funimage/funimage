(ns funimage.imp.roi;; This is the namespace for ImagePlus utilities (see img for imglib2 img's)
  (:use [funimage imp])
  (:require [clojure.string :as string]
            [clojure.java.io :as io])
  (:import [java.io File]
           [ij IJ ImagePlus ImageStack]
           [ij.io FileSaver]

           [ij.gui NewImage Toolbar Roi OvalRoi]
           [ij.process ImageProcessor ByteProcessor ImageStatistics]
           [ij.measure Calibration]
           [ij.plugin.frame RoiManager]
           
           [ij.io Opener]           
           
           [net.imglib2.img ImagePlusAdapter Img]
           [net.imglib2.img.display.imagej ImageJFunctions]
           [net.imglib2.type NativeType]
           [net.imglib2.type.numeric NumericType ARGBType]
           [net.imglib2.type.numeric.real FloatType]
           [net.imglib2.algorithm.gauss3 Gauss3]
           [net.imglib2.algorithm.dog DifferenceOfGaussian]
           [net.imglib2.view Views IntervalView]
           [net.imglib2 Cursor RandomAccess]))

(defn open-roi
  "Open an ROI from filename"
  [filename]
  ^Roi (ij.io.RoiDecoder/open filename))

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

#_(defn color-mask-rois
   "Make a colored mask of ROIs, expects: [[roi1 color1], [roi2 color2], ...]"
   [imp rois colors]
   (let [mask (create-imp-like imp)]
     (.setSnapshotCopyMode (.getProcessor mask) false)
     (dotimes [k (count rois)]      
       (set-roi mask (nth rois k))      
       (set-fill-value mask (nth colors k))
       (.fillPolygon ^ij.process.ImageProcessor (.getProcessor mask) ^java.awt.Polygon (.getPolygon (nth rois k))))
     mask))

(defn fill-rois
  "Fill ROIs, colors should by the same format as imp, which probably has to be single channel at the moment."
  [imp rois colors]
  (let [mask (create-imp-like imp)]
    (.setSnapshotCopyMode (.getProcessor mask) false)
    (dotimes [k (count rois)]      
      (.setColor ^ij.process.ImageProcessor (.getProcessor mask) (nth colors k))
      (.fill ^ij.process.ImageProcessor (.getProcessor mask) ^ij.gui.Roi (nth rois k)))
    mask))

(defn fill-rois-convex-hull
  "Fill ROIs, colors should by the same format as imp, which probably has to be single channel at the moment."
  [imp rois colors]
  (let [mask (create-imp-like imp)]
    (.setSnapshotCopyMode (.getProcessor mask) false)
    (dotimes [k (count rois)]      
      (.setColor ^ij.process.ImageProcessor (.getProcessor mask) (nth colors k))
      (.fillPolygon ^ij.process.ImageProcessor (.getProcessor mask) ^java.awt.Polygon (.getConvexHull (nth rois k)))
      #_(.fill ^ij.process.ImageProcessor (.getProcessor mask) ^ij.gui.Roi (nth rois k)))
    mask))

(defn roi-angle
  "Get the angle of a ROI."
  [^ij.gui.Roi roi]
  (.getAngle roi))

(defn rois-angle
  "Get the angles of each ROI."
  [rois]
  (doall (map roi-angle rois)))

(defn roi-ferets-diameter
  "Return Feret's diameter for each ROI. This is the greatest distance between any 2 points along the perimeter/ROI boundary."
  [^ij.gui.Roi roi]
  (.getFeretsDiameter  roi))

(defn rois-ferets-diameter
  "Return Feret's diameter for each ROI. This is the greatest distance between any 2 points along the perimeter/ROI boundary."
  [rois]
  (doall (map roi-ferets-diameter rois)))

(defn roi-perimeter-length
  "Return the perimeter length of each ROI."
  [^ij.gui.Roi roi]
  (.getLength ^ij.gui.Roi roi))

(defn rois-perimeter-length
  "Return the perimeter length of each ROI."
  [rois]
  (doall (map roi-perimeter-length rois)))
  
(defn poly-area
  "Return the area of a polygon."
  [^java.awt.Polygon poly]
  (let [min-x (apply min (.xpoints poly))
        max-x (apply max (.xpoints poly))
        min-y (apply min (.ypoints poly))
        max-y (apply max (.ypoints poly))]        
    (loop [x min-x
           y min-y
           tally 0]
      (cond (and (> x max-x)
                 (> y max-y)); finish at end of region
            tally
            (> x max-x); line wrap
            (recur min-x (inc y) tally)
            :else
            (recur (inc x) y 
                   (if (.contains poly x y) (inc tally) tally))))))

#_(defn roi-area
    "Compute the area of an roi using an integer grid + tally mechanism."
    [roi]
    (let [poly (.getFloatPolygon roi)]
      (poly-area poly)))

#_(defn roi-area
  "Compute the area of an roi using an integer grid + tally mechanism."
  [roi]
  (let [poly ^ij.process.FloatPolygon (.getFloatPolygon roi)
        min-x (apply min (.xpoints poly))
        max-x (apply max (.xpoints poly))
        min-y (apply min (.ypoints poly))
        max-y (apply max (.ypoints poly))]        
    (loop [x min-x
           y min-y
           tally 0]
      (cond (and (> x max-x)
                 (> y max-y)); finish at end of region
            tally
            (> x max-x); line wrap
            (recur min-x (inc y) tally)
            :else
            (recur (inc x) y 
                   (if (.contains poly x y) (inc tally) tally))))))

(defn roi-area
  "Compute the area of an roi using an integer grid + tally mechanism."
  [roi]
  (let [poly ^ij.process.FloatPolygon (.getFloatPolygon roi)
        min-x (apply min (.xpoints poly))
        max-x (apply max (.xpoints poly))
        min-y (apply min (.ypoints poly))
        max-y (apply max (.ypoints poly))
        
        xpoints (.xpoints poly)
        ypoints (.ypoints poly)]
    
    (/ (apply + (map (fn [x1 y2 y1 x2]
                       (- (* x1 y2) (* y1 x2)))
                     xpoints (concat (rest ypoints) [(first ypoints)])
                     ypoints (concat (rest xpoints) [(first xpoints)])))
       2)))

(defn centroid
  "Get centroid of a ROI."
  [roi]
  (let [poly ^ij.process.FloatPolygon (.getFloatPolygon roi)
        min-x (apply min (.xpoints poly))
        max-x (apply max (.xpoints poly))
        min-y (apply min (.ypoints poly))
        max-y (apply max (.ypoints poly))
        
        xpoints (.xpoints poly)
        ypoints (.ypoints poly)
        area (roi-area roi)
        cx (/ (apply +
                     (for [idx (range (dec (count xpoints)))]
                       (* (+ (nth xpoints idx)
                             (nth xpoints (inc idx)))
                          (- (* (nth xpoints idx)
                                (nth ypoints (inc idx)))
                             (* (nth xpoints (inc idx))
                                (nth ypoints idx))))))
             (* 6 area))
        cy (/ (apply +
                     (for [idx (range (dec (count xpoints)))]
                       (* (+ (nth ypoints idx)
                             (nth ypoints (inc idx)))
                          (- (* (nth xpoints idx)
                                (nth ypoints (inc idx)))
                             (* (nth xpoints (inc idx))
                                (nth ypoints idx))))))
             (* 6 area))]
    [cx cy]))
    
(defn overlaps?
  "Do 2 ROIs overlap?"
  [roi-a roi-b]
  (let [poly-a ^ij.process.FloatPolygon (.getFloatPolygon roi-a)
        xpoints-a (.xpoints poly-a)
	      ypoints-a (.ypoints poly-a)
        
	      min-x-a (apply min xpoints-a)
	      max-x-a (apply max xpoints-a)
	      min-y-a (apply min ypoints-a)
	      max-y-a (apply max ypoints-a)
	      
       
        poly-b ^ij.process.FloatPolygon (.getFloatPolygon roi-b)
	      xpoints-b (.xpoints poly-b)
	      ypoints-b (.ypoints poly-b)
        
	      min-x-b (apply min xpoints-b)
	      max-x-b (apply max xpoints-b)
	      min-y-b (apply min ypoints-b)
	      max-y-b (apply max ypoints-b)
	      	      
	      a-contains-b (for [idx (range (count xpoints-a))]
                      (.contains poly-b
                        (nth xpoints-a idx)
                        (nth ypoints-a idx)))
        b-contains-a (for [idx (range (count xpoints-b))]
                       (.contains poly-a
                         (nth xpoints-b idx)
                         (nth ypoints-b idx)))]
    (or (reduce #(or %1 %2)
                a-contains-b)
        (reduce #(or %1 %2)
                b-contains-a))))

(defn clear-outside-roi
  "Clear the image outside the roi. Set the background color beforehand"
  [imp roi]
  (let [prev-roi (get-roi imp)]
    (set-roi imp roi)
    (ij.IJ/run imp "Clear Outside" "slice")
    (set-roi imp prev-roi)))

(defn clear-roi
  "Clear the image outside the roi. Set the background color beforehand"
  [imp roi]
  (let [prev-roi (get-roi imp)]
    (set-roi imp roi)
    (ij.IJ/run imp "Clear" "slice")
    (set-roi imp prev-roi)))
