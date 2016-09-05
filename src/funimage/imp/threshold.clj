(ns funimage.imp.threshold
  (:use [funimage imp conversion])
  (:require [clojure.string :as string]
            [clojure.java.io :as io])
  (:import [java.io File]
           [ij IJ ImagePlus ImageStack]
           [ij.io FileSaver]
           [ij.gui NewImage Toolbar Roi]
           [ij.process ImageProcessor ByteProcessor ImageStatistics]
           [ij.measure Calibration]
           
           [ij.io Opener]
           ;[io.scif.img ImgOpener]
                     
           [net.imglib2.img ImagePlusAdapter Img]
           [net.imglib2.img.display.imagej ImageJFunctions]
           [net.imglib2.type NativeType]
           [net.imglib2.type.numeric NumericType ARGBType]
           [net.imglib2.type.numeric.real FloatType]
           [net.imglib2.algorithm.gauss3 Gauss3]
           [net.imglib2.algorithm.dog DifferenceOfGaussian]
           [net.imglib2.view Views IntervalView]
           [net.imglib2 Cursor RandomAccess]
           
           [fiji.threshold Auto_Threshold]))

(def threshold-methods [:IJ-default :huang :intermodes :iso-data :li :max-entropy :mean :min-error-i :minimum :moments :otsu :percentile :renyi-entropy :shanbhag :triangle :yen])

(defn thresholdable-imp?
  "Check if an imageplus is thresholdable based on the pixel type."
  [imp]
  (or (= (get-type-string imp) "16-bit")
      (= (get-type-string imp) "8-bit")))

(defn autothreshold-level
  "Get autothreshold level."
  [data method-name]
  (cond (= method-name :IJ-default) (Auto_Threshold/IJDefault data)
        (= method-name :huang) (Auto_Threshold/Huang data)
        (= method-name :intermodes) (Auto_Threshold/Intermodes data)
        (= method-name :iso-data) (Auto_Threshold/IsoData data)
        (= method-name :li) (Auto_Threshold/Li data)
        (= method-name :max-entropy) (Auto_Threshold/MaxEntropy data)
        (= method-name :mean) (Auto_Threshold/MaxEntropy data)
        (= method-name :min-error-i) (Auto_Threshold/MinErrorI data)
        (= method-name :minimum) (Auto_Threshold/Minimum data)
        (= method-name :moments) (Auto_Threshold/Moments data)
        (= method-name :otsu) (Auto_Threshold/Otsu data)
        (= method-name :percentile) (Auto_Threshold/Percentile data)
        (= method-name :renyi-entropy) (Auto_Threshold/RenyiEntropy data)
        (= method-name :shanbhag) (Auto_Threshold/Shanbhag data)
        (= method-name :triangle) (Auto_Threshold/Triangle data)
        (= method-name :yen) (Auto_Threshold/Yen data)))    

#_(defn autothreshold
   "Autotheshold an imageplus with the supplied method.
Valid method names:
:IJ-default, :huang, :intermodes, :iso-data, :li, :max-entropy, :mean, :min-error-i, :minimum, :moments, :otsu, :percentile, :renyi-entropy, :shanbhag, :triangle, :yen
This list is programmatically accessible as: funimage.imp.threshold/threshold-methods"
   [^ImagePlus imp method-name noWhite noBlack doIwhite doIset doIlog doIstackHistogram]
   (when (and (not (nil? imp)) 
              (thresholdable-imp? imp))
     (let [current-slice (.getCurrentSlice imp)
           ip ^ImageProcessor (.getProcessor imp)
           xe (.getWidth ip)
           ye (.getHeight ip)
           ;int x, y, c=0;
           c 0
           b (if (= (.getBitDepth imp) 8) 255 65535)
           [c b] (if doIwhite [b 0] [c b])
           data ^ints (.getHistogram ip)
           ;temp = new int [data.length];
           ]
       (IJ/showStatus "Thresholding...")
       ; Undo, and stack histogram stuff would be here
       ;if (noBlack) data[0]=0;
       ;if (noWhite) data[data.length - 1]=0;
       (let [minbin (some #(when (pos? (nth data %)) %) (range (count data)))
             maxbin (some #(when (pos? (nth data %)) %) (reverse (range (count data))))
             data2 (int-array (drop minbin (take maxbin data)))
             threshold (+ minbin (autothreshold-level data2 method-name))]
         (println "Threshold = " threshold)
         (if doIset 
           (if doIwhite
             (.setThreshold (.getProcessor imp) (inc threshold) (dec (count data)) ImageProcessor/RED_LUT)
             (.setThreshold (.getProcessor imp) 0 threshold ImageProcessor/RED_LUT))
           (.threshold (.getProcessor imp) (int threshold)))    
         imp
           #_(img->imp 
              (first (walk-imgs
                       (fn [^Cursor cur] (.set (.get cur) 
                                           (if (> (.getRealFloat (.get cur)) threshold) 255 0)))
                  (imp->img imp))))))))

(defn autothreshold
   "Autotheshold an imageplus with the supplied method.
Valid method names:
:IJ-default, :huang, :intermodes, :iso-data, :li, :max-entropy, :mean, :min-error-i, :minimum, :moments, :otsu, :percentile, :renyi-entropy, :shanbhag, :triangle, :yen
This list is programmatically accessible as: funimage.imp.threshold/threshold-methods"
   [^ImagePlus imp method-name noWhite noBlack doIwhite doIset doIlog doIstackHistogram]
   (when (and (not (nil? imp)) 
              (thresholdable-imp? imp))
     (let [stack ^ImageStack (.getStack imp)
           stack-histogram (when doIstackHistogram
                             (.histogram (ij.process.StackStatistics. imp)))]
       (dotimes [k (.getSize stack)]                
         (let [current-slice (inc k) #_(.getCurrentSlice imp)
               ip ^ImageProcessor (.getProcessor stack (inc k))
               xe (.getWidth ip)
               ye (.getHeight ip)
               ;int x, y, c=0;
               c 0
               b (if (= (.getBitDepth imp) 8) 255 65535)
               [c b] (if doIwhite [b 0] [c b])
               data ^ints (if doIstackHistogram
                            stack-histogram
                            (.getHistogram ip))
               ;temp = new int [data.length];
               ]
           (IJ/showStatus "Thresholding...")
           ; Undo, and stack histogram stuff would be here
           ;if (noBlack) data[0]=0;
           ;if (noWhite) data[data.length - 1]=0;
           (let [minbin (some #(when (pos? (nth data %)) %) (range (count data)))
                 maxbin (some #(when (pos? (nth data %)) %) (reverse (range (count data))))
                 data2 (int-array (drop minbin (take maxbin data)))
                 threshold (+ minbin (autothreshold-level data2 method-name))]
             ;(println "Threshold = " threshold)
             (if doIset 
               (if doIwhite
                 (.setThreshold ip (inc threshold) (dec (count data)) ImageProcessor/RED_LUT)
                 (.setThreshold ip 0 threshold ImageProcessor/RED_LUT))
               (.threshold ip (int threshold)))))))
     imp))

  
#_(do 
   (def imp (convert-to-8bit (open-imp "hello-communist-kitty_bw.tif")))
   (def mask (autotheshold imp :otsu false false true false false false))
   (show-imp imp)
   (show-imp mask))

#_(let [imp (convert-to-8bit (open-imp "hello-communist-kitty_bw.tif"))      
       mask (autotheshold imp :otsu false false true true false false)]
   (show-imp imp)
  
   #_(show-imp mask))
  
      