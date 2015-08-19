(ns funimage.imp.filters
  (:use [funimage imp]
        [funimage.imp calculator])
  (:require [clojure.string :as string]
            [clojure.java.io :as io])
  (:import [java.io File]
           [ij IJ ImagePlus ImageStack]
           [loci.plugins BF]
           [ij.io FileSaver]
           [javax.media.j3d Transform3D]
           [javax.vecmath Vector3f Point3f Quat4f]
           [ij.gui NewImage Toolbar]
           [ij.process ImageProcessor ByteProcessor ImageStatistics FHT ]
           [ij.measure Calibration]
           
           ;[ij.plugin FFT]
           ;[funimage Fast_FourierTransform]
           ;[registration3d Fast_FourierTransform]
           
           [org.jtransforms.fft FloatFFT_3D]
           
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

(defn pad-power-of-2
  "Pad the image dimension to a power of 2."
  [imp]
  (let [^ImageProcessor ip (.getProcessor imp)
        originalWidth (.getWidth ip)
        originalHeight (.getHeight ip)
        originalDepth (.getNSlices imp)
        maxN (max originalWidth originalHeight);
        i (loop [i 2] (if (< i maxN) (recur (* 2 i)) i))]
    (if (and (= i maxN) (= originalWidth originalHeight)
             (zero? (mod originalDepth 2)))
      imp      
      (let [^ImageStatistics stats (ImageStatistics/getStatistics ip ImageStatistics/MEAN nil)
            newDepth (loop [i 2] (if (< i originalDepth) (recur (* 2 i)) i))
            ^ImageStack stack (ImageStack. i i newDepth)]            
        (dotimes [k newDepth]
          (let [^ImageProcessor ip2 (.createProcessor ip i i )]
            (.setValue ip2 (.mean stats))
            (.fill ip2)
            (when (< k originalDepth)
              (.setSlice imp (inc k))
              (let [^ImageProcessor ip (.getProcessor imp)]
                (.insert ip2 ip 0 0)))
            (.addSlice stack ip2)))
        (ImagePlus. (str "Padded " (.getTitle imp)) stack)))))

(defn fft3d
  "Do a 3d FFT"
  [imp]
  (let [imp (pad-power-of-2 imp)
        fft (FloatFFT_3D. (get-stack-depth imp) (get-width imp) (get-height imp))
        outimp (imp-copy-create imp imp)
        data (make-array Float/TYPE (get-stack-depth imp) (get-width imp) (get-height imp))
        #_(double-array (.getVoxels (.getStack imp) 0 0 0 (get-width imp) (get-height imp) (get-stack-depth imp) (float-array (* (get-width imp) (get-height imp) (get-stack-depth imp)))))]
    (dotimes [k (get-stack-depth imp)]
      (.setSlice imp k)
      (aset data k (.getFloatArray (.getProcessor imp))))
    (.realForward fft data)
    (dotimes [k (get-stack-depth imp)]
      (.setSlice outimp k)
      (.setFloatArray (.getProcessor imp) (aget data k)))
    outimp))

; Registration3d version

#_(defn pad-power-of-2
   "Pad the image dimension to a power of 2."
   [imp]
   (let [^ImageProcessor ip (.getProcessor imp)
         originalWidth (.getWidth ip)
         originalHeight (.getHeight ip)
         maxN (max originalWidth originalHeight);
         i (loop [i 2] (if (< i maxN) (recur (* 2 i)) i))]
     (if (and (= i maxN) (= originalWidth originalHeight))
       imp      
       (let [^ImageStatistics stats (ImageStatistics/getStatistics ip ImageStatistics/MEAN nil)
             ^ImageProcessor ip2 (.createProcessor ip i i)]
         (.setValue ip2 (.mean stats))
         (.fill ip2)
         (.insert ip2 ip 0 0);
         (ImagePlus. (str "Padded " (.getTitle imp))  ip2)))))

#_(defn fft3 
   "FFT of an imageplus."
   [imp]
   (let [^Fast_FourierTransform fft (Fast_FourierTransform.)
         imp (pad-power-of-2 imp)
         flt-ary (.StackToFloatArray fft (.getStack imp))]
     (.computeFFT fft flt-ary false)
     (.FloatArrayToStack fft (str "FFT of " (.getTitle imp)) 0 0)))
  
