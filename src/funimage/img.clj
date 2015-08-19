; This is the namespace for imglib2 img's (see imp for ImagePlus)
(ns funimage.img
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
           ;[io.scif.img ImgOpener]
           
           [java.util.concurrent Executors]
           [java.awt Canvas Graphics]
           [javax.swing JFrame JMenu JMenuBar JMenuItem]
           
           [net.imglib2.algorithm.neighborhood Neighborhood RectangleShape]
           [net.imglib2.util Intervals]
           [net.imglib2.img ImagePlusAdapter Img]
           [net.imglib2.img.display.imagej ImageJFunctions]
           [net.imglib2.type NativeType]
           [net.imglib2.type.numeric NumericType ARGBType]
           [net.imglib2.type.numeric.real FloatType]
           [net.imglib2.algorithm.gauss3 Gauss3]
           [net.imglib2.algorithm.dog DifferenceOfGaussian]
           [net.imglib2.view Views IntervalView]
           [net.imglib2 Cursor RandomAccess RandomAccessibleInterval Interval]))

(defn walk-imgs
  "Walk all images (as cursors) applying f at each step. 
f is a function that operates on cursors in the same order as imgs
If you have an ImagePlus, then use funimage.conversion"
  ([f img1]
    (let [cur1 (.cursor ^Img img1)]
      (loop []
        (when (.hasNext ^Cursor cur1)
          (.fwd ^Cursor cur1)
          (f cur1)
          (recur)))
      [img1]))
  ([f img1 img2]
    (let [cur1 (.cursor ^Img img1)
          cur2 (.cursor ^Img img2)]
      (loop []
        (when (and (.hasNext ^Cursor cur1)
                   (.hasNext ^Cursor cur2))
          (.fwd ^Cursor cur1)
          (.fwd ^Cursor cur2)
          (f cur1 cur2)
          (recur)))
      [img1 img2]))
  ([f img1 img2 & imgs]
    (let [imgs (concat [img1 img2] imgs)
          curs (map #(.cursor ^Img %) imgs)]
      (loop []
        (when (every? (map #(.hasNext ^Cursor %) curs))                           
          (doseq [cur curs] (.fwd ^Cursor cur))
          (apply f curs)
          (recur)))
      imgs)))

(defn replace-img
  "Replace img1 with img2"
  [img1 img2]
  (second (walk-imgs 
            (fn [^Cursor cur1 ^Cursor cur2] (.set (.get cur2) (.get cur1)))
            img2 img1)))
    
(defn subtract-img
  "Subtract the second image from the first (destructive)."
  [img1 img2]
  (first (walk-imgs
           (fn [^Cursor cur1 ^Cursor cur2] (.sub (.get cur2) (.get cur1)))
           img1 img2)))

(defn elmul-img
  "Subtract the second image from the first (destructive)."
  [img1 img2]
  (first (walk-imgs
           (fn [^Cursor cur1 ^Cursor cur2] (.mul (.get cur2) (double (.get (.get cur1)))))
           img1 img2)))

(defn threshold-img
  "Convert an image into a binary one about a threshold."
  ([img threshold]
    (threshold-img img threshold 0 255))
  ([img threshold low high]
    (let [f-min (float low)
          f-max (float high)]
      (first (walk-imgs
               (fn [^Cursor cur] (.set (.get cur) 
                                   (if (> (.getRealFloat (.get cur)) threshold) f-max f-min)))
               img)))))

(defn sum-img
  "Take the sum of all pixel values in an image."
  [img]
  (let [sum (atom 0)]
    (walk-imgs
      (fn [^Cursor cur]
        (swap! sum + (.getRealFloat (.get cur)))))
    @sum))

#_(defn replace-subimg
    "Replace a subimage of a larger image with a smaller one."
    [img replacement start-x start-y]
    (let [offset (long-array [start-x start-y])
          rep-dim (image-dimensions replacement)
          stop-point (long-array (map #(dec (+ %1 %2)) offset rep-dim))
          subimg (Views/interval img offset stop-point)]
     (let [cur (.cursor ^Img replacement)
           ra (.randomAccess ^IntervalView subimg)
           pos (long-array 2)]
       (doseq [el rep-dim] (print el " ")) (println)
       (doseq [el (image-dimensions subimg)] (print el " ")) (println)
       (dotimes [k 2] (print (.min subimg k) " ")) (println )
       (dotimes [k 2] (print (.max subimg k) " ")) (println) 
       (loop []
         (when (.hasNext ^Cursor cur)
           (.fwd ^Cursor cur)
           (.localize ^Cursor cur ^longs pos)
           (.setPosition ^RandomAccess ra 
              ^longs pos)
           (.set (.get ra) (.get (.get cur)))
           (recur))))
     img))

(defn fill-boundary
  "Fill boundary pixels with the given value.
(bx,by,bz) - 'bottom' point. these are the small values. exclusive
(tx,ty,tz) - 'top' point. these are the big values. exclusive
locations outside these points are assigned fill-value"
  [img bx by bz tx ty tz fill-value]
  (let [location (float-array [0 0 0])
        f-fv (float fill-value)]
    (first (walk-imgs
             (fn [^Cursor cur]               
               (.localize cur location)
               (when (or (< (first location) bx) (< (second location) by) (< (last location) bz)
                         (> (first location) tx) (> (second location) ty) (> (last location) tz))
                 (.set (.get cur) 
                   f-fv)))
             img))))
 
#_(defn neighborhood-walk-img
   "Do a neighborhood walk over an imglib2 img.
Rectangle only"
   ([f radius img]
     (let [interval ^Interval (Intervals/expand img (* -1 radius))
           source ^RandomAccessibleInterval (Views/interval img interval)
           center ^Cursor (.cursor (Views/iterable source))
           shape ^RectangleShape (RectangleShape. radius true)]
       (doseq [^Neighborhood local-neighborhood (.neighborhoods shape source)] 
         (do (.next center)
           (f center local-neighborhood)))
       img)))

(defn neighborhood-walk-img-to-center
  "Do a neighborhood walk over an imglib2 img.
Rectangle only"
  ([f radius source dest]
    (let [interval ^Interval (Intervals/expand source (* -1 radius))
          source ^RandomAccessibleInterval (Views/interval source interval)
          dest ^RandomAccessibleInterval (Views/interval dest interval)
          center ^Cursor (.cursor (Views/iterable dest))
          shape ^RectangleShape (RectangleShape. radius false)]
      (doseq [^Neighborhood local-neighborhood (.neighborhoods shape source)] 
        (do (.fwd center)
          (f center local-neighborhood)))
      dest)))


#_(defn mean-filter-nonzero
    "Mean filter with imglib2 with a square kernel"
    [imp radius]
    (let [nhood-size (* radius radius)]
      (img->imp
        (neighborhood-walk-img-to-center 
                         (fn [^net.imglib2.Cursor cur ^net.imglib2.algorithm.neighborhood.Neighborhood nhood]
                           (.set ^net.imglib2.type.numeric.real.FloatType (.get cur)
                             (float (let [pxs (filter #(not (zero? %)) (map #(.get ^net.imglib2.type.numeric.real.FloatType %) nhood))]
                                      (if (empty? pxs) 0
                                        (/ (apply + pxs)
                                           (count pxs)))))))
                         radius
                         (imp->img imp) (imp->img (create-imp :title (str "closest angle")
                                                              :type "32-bit"
                                                              :width (get-width imp)
                                                              :height (get-height imp)))))))

#_(defn median-filter-nonzero
     "Median filter with imglib2 with a square kernel"
     [imp radius]
     (let [nhood-size (* radius radius)]
       (img->imp
         (neighborhood-walk-img-to-center 
                          (fn [^net.imglib2.Cursor cur ^net.imglib2.algorithm.neighborhood.Neighborhood nhood]
                            (.set ^net.imglib2.type.numeric.real.FloatType (.get cur)
                              (float (let [pxs (filter #(not (zero? %)) (map #(.get ^net.imglib2.type.numeric.real.FloatType %) nhood))]
                                       (if (empty? pxs) 0
                                         (nth pxs (int (/ (count pxs) 2))))))))
                          radius
                          (imp->img imp) (imp->img (create-imp :title (str "closest angle")
                                                               :type "32-bit"
                                                               :width (get-width imp)
                                                               :height (get-height imp)))))))

#_(defn variance-filter-nonzero
      "Median filter with imglib2 with a square kernel"
      [imp radius]
      (let [nhood-size (* radius radius)]
        (img->imp
          (neighborhood-walk-img-to-center 
                           (fn [^net.imglib2.Cursor cur ^net.imglib2.algorithm.neighborhood.Neighborhood nhood]
                             (.set ^net.imglib2.type.numeric.real.FloatType (.get cur)
                               (float (let [pxs (filter #(not (zero? %)) (map #(.get ^net.imglib2.type.numeric.real.FloatType %) nhood))]
                                        (if (empty? pxs) 0 (/ (+ (apply min pxs) (apply max pxs)) 2))))))
                           radius
                           (imp->img imp) (imp->img (create-imp :title (str "closest angle")
                                                                :type "32-bit"
                                                                :width (get-width imp)
                                                                :height (get-height imp)))))))
