; This is the namespace for imglib2 img's (see imp for ImagePlus)
(ns funimage.img
  ;(:use [funimage.img cursor])
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [funimage.img.cursor :as cursor])
  (:import [net.imglib2.algorithm.neighborhood Neighborhood RectangleShape]
           [net.imglib2.util Intervals]
           [net.imglib2.img ImagePlusAdapter Img]
           [net.imglib2.img.display.imagej ImageJFunctions]
           [net.imglib2.type NativeType]
           [net.imglib2.type.numeric NumericType ARGBType]
           [net.imglib2.type.numeric.real FloatType]
           [net.imglib2.algorithm.gauss3 Gauss3]
           [net.imglib2.algorithm.dog DifferenceOfGaussian]
           [net.imglib2.view Views IntervalView]
           [net.imglib2 Cursor RandomAccess RandomAccessibleInterval Interval]
           [net.imglib2.algorithm.binary Thresholder]
           ))

(defn show
  "Display an Img."
  [^Img img]
  (net.imglib2.img.display.imagej.ImageJFunctions/show img))

(defn copy
  "Create a copy of an img."
  [^Img img]
  ^Img (.copy img))

(defn create-img-like
  "Create an Img like the input."
  ([^Img img tpe]
    ^Img (.create (.factory img)
           img
           tpe))
  ([^Img img]
    (create-img-like img (.firstElement img))))

(defn get-size-dimension
  "Return the size along the specified dimension."
  [^Img img d]
  (.dimension img d))

(defn get-width
  "Return the width of the img."
  [^Img img]
  (.dimension img 0))

(defn get-height
  "Return the height of the img."
  [^Img img]
  (.dimension img 1))

(defn get-depth
  "Return the depth of the image."
  [^Img img]
  (.dimension img 2))

(defn get-type
  "Return the class type of an image."
  [^Img img]
  (net.imglib2.util.Util/getTypeFromInterval img))

(defn map-img
   "Walk all images (as cursors) applying f at each step.
f is a function that operates on cursors in the same order as imgs
If you have an ImagePlus, then use funimage.conversion
Note: this uses threads to avoid some blocking issues."
   ([f img1]
     (let [cur1 (.cursor ^Img img1)
           t (Thread.
               (fn []
                 (loop []
                   (when (.hasNext ^Cursor cur1)
                     (.fwd ^Cursor cur1)
                     (f cur1)
                     (recur)))))]
       (.start t)
       (.join t)
       [img1]))
   ([f img1 img2]
     (let [cur1 (.cursor ^Img img1)
           cur2 (.cursor ^Img img2)
           t (Thread.
               (fn []
                 (loop []
                   (when (and (.hasNext ^Cursor cur1)
                              (.hasNext ^Cursor cur2))
                     (.fwd ^Cursor cur1)
                     (.fwd ^Cursor cur2)
                     (f cur1 cur2)
                     (recur)))))]
       (.start t)
       (.join t)
       [img1 img2]))
   ([f img1 img2 & imgs]
     (let [imgs (concat [img1 img2] imgs)
           curs (map #(.cursor ^Img %) imgs)
           t (Thread.
               (fn []
                 (loop []
                   (when (every? #(.hasNext ^Cursor %) curs)
                     (doseq [cur curs] (.fwd ^Cursor cur))
                     (apply f curs)
                     (recur)))))]
       (.start t)
       (.join t)
       imgs)))

(defn map-localize-img
   "Walk all images (as cursors) applying f at each step.
f is a function that operates on cursors in the same order as imgs
If you have an ImagePlus, then use funimage.conversion
Note: this uses threads to avoid some blocking issues."
   ([f img1]
     (let [cur1 (.localizingCursor ^Img img1)
           t (Thread.
               (fn []
                 (loop []
                   (when (.hasNext ^Cursor cur1)
                     (.fwd ^Cursor cur1)
                     (f cur1)
                     (recur)))))]
       (.start t)
       (.join t)
       [img1]))
   ([f img1 img2]
     (let [cur1 (.localizingCursor ^Img img1)
           cur2 (.localizingCursor ^Img img2)
           t (Thread.
               (fn []
                 (loop []
                   (when (and (.hasNext ^Cursor cur1)
                              (.hasNext ^Cursor cur2))
                     (.fwd ^Cursor cur1)
                     (.fwd ^Cursor cur2)
                     (f cur1 cur2)
                     (recur)))))]
       (.start t)
       (.join t)
       [img1 img2]))
   ([f img1 img2 & imgs]
     (let [imgs (concat [img1 img2] imgs)
           curs (map #(.localizingCursor ^Img %) imgs)
           t (Thread.
               (fn []
                 (loop []
                   (when (every? #(.hasNext ^Cursor %) curs)
                     (doseq [cur curs] (.fwd ^Cursor cur))
                     (apply f curs)
                     (recur)))))]
       (.start t)
       (.join t)
       imgs)))

; Nonthreaded version
#_(defn map-imgs
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

(defn replace
  "Replace img1 with img2"
  [^Img img1 ^Img img2]
  (second (map-img
            (fn [^Cursor cur1 ^Cursor cur2] (.set (.get cur2) (.get cur1)))
            img2 img1)))

(defn subtract
  "Subtract the second image from the first (destructive)."
  [^Img img1 ^Img img2]
  (first (map-img cursor/sub img1 img2)))

(defn elmul
  "Subtract the second image from the first (destructive)."
  [^Img img1 ^Img img2]
  (first (map-img cursor/mul img1 img2)))

(defn scale
  "Scale the image."
  [^Img img scalar]
  (first (map-img #(cursor/set-val (* (cursor/get-val %) scalar)) img)))

(defn threshold
  "Binarize an image about a threshold"
  [^Img img threshold]
  (Thresholder/threshold img
                         (let [tval (.copy (get-type img))]
                           (.set tval threshold)
                           tval)
                         true
                         1))

(defn sum
  "Take the sum of all pixel values in an image."
  [^Img img]
  (let [sum (atom 0)]
    (map-img (fn [^Cursor cur] (swap! sum + (cursor/get-val cur))) img)
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
  [^Img img bx by bz tx ty tz fill-value]; should take array of locations to generalize to N-D
  (let [location (float-array [0 0 0])
        f-fv (float fill-value)]
    (first (map-img (fn [^Cursor cur]
                      (.localize cur location)
                      (when (or (< (first location) bx) (< (second location) by) (< (last location) bz)
                                (> (first location) tx) (> (second location) ty) (> (last location) tz))
                        (.set (.get cur)
                          f-fv)))
             img))))


(defn neighborhood-map-to-center
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

(defn periodic-neighborhood-map-to-center
  "Do a neighborhood walk over an imglib2 img.
Rectangle only"
  ([f radius source dest]
    (let [source (net.imglib2.view.Views/interval (net.imglib2.view.Views/extendPeriodic source) dest) ;^net.imglib2.view.ExtendedRandomAccessibleInterval
          center ^net.imglib2.Cursor (.cursor (net.imglib2.view.Views/iterable dest))
          shape ^net.imglib2.algorithm.neighborhood.RectangleShape (net.imglib2.algorithm.neighborhood.RectangleShape. radius false)
          local-neighborhood ^net.imglib2.algorithm.neighborhood.Neighborhood (.neighborhoods shape ^net.imglib2.RandomAccessibleInterval source)
          neighborhood-cursor ^net.imglib2.Cursor (.cursor local-neighborhood)]
      (loop []
        (when (.hasNext ^net.imglib2.Cursor center)
          (.fwd ^net.imglib2.Cursor center)
          (.fwd ^net.imglib2.Cursor neighborhood-cursor) 
          (f center ^net.imglib2.algorithm.neighborhood.RectangleNeighborhoodUnsafe (.get ^net.imglib2.Cursor neighborhood-cursor))
          (recur)))
      dest)))

(defn replace-subimg
  "Replace a subimage of a larger image with a smaller one."
  [img replacement start-position]
  (let [offset (long-array start-position)
        replacement-dim (long-array (count start-position))
        img-dim (long-array (count start-position))]
    (.dimensions replacement replacement-dim)
    (.dimensions img img-dim)
    (let [stop-point (long-array (map #(dec (+ %1 %2)) offset replacement-dim))
          subimg (Views/interval img offset stop-point)
          cur (.cursor ^Img replacement)
          ra (.randomAccess ^IntervalView subimg)
          pos (long-array (count start-position))]
      (map-img cursor/copy subimg replacement)))
    img)

(defn replace-subimg-with-opacity
  "Replace a subimage of a larger image with a smaller one if the replacement is greater than the provided opacity value."
  [img replacement start-position opacity]
  (let [offset (long-array start-position)
        replacement-dim (long-array (count start-position))
        img-dim (long-array (count start-position))]
    (.dimensions replacement replacement-dim)
    (.dimensions img img-dim)
    (let [stop-point (long-array (map #(dec (+ %1 %2)) offset replacement-dim))
          subimg (Views/interval img offset stop-point)
          cur (.cursor ^Img replacement)
          ra (.randomAccess ^IntervalView subimg)
          pos (long-array (count start-position))]
      (map-img 
        (fn [^Cursor cur1 ^Cursor cur2] 
          (when (> (.get (.get cur2)) opacity)
            (.set ^net.imglib2.type.numeric.RealType (.get cur1) (.get cur2))))
        subimg replacement)))
    img)

(defn get-val
  "Return the value at a given position."
  [^Img img ^longs position]
  (let [^RandomAccess ra (.randomAccess img)]
    (.setPosition ra position)
    (.get (.get ra))))

(defn set-val
  "Set the value at a given position."
  [^Img img ^longs position new-val]
  (let [^RandomAccess ra (.randomAccess img)]
    (.setPosition ra position)
    (.set (.get ra) new-val))
  img)
