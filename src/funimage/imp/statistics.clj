(ns funimage.imp.statistics  
  (:require [clojure.string :as string]
            [clojure.java.io :as io])
  (:import [java.io File]
           [ij IJ ImagePlus ImageStack]
           [loci.plugins BF]
           [ij.io FileSaver]
           [javax.media.j3d Transform3D]
           [javax.vecmath Vector3f Point3f Quat4f]
           [ij.gui NewImage Toolbar Roi]
           [ij.process ImageProcessor ByteProcessor ImageStatistics]
           [ij.measure Calibration]
           [ij.plugin Duplicator]
           
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

(defn get-available-measurements
  "Return a sequence of keywords corresponding to available measurements."
  []
  [:add-to-overlay  :area  :area-fraction  :center-of-mass  :centroid  :circularity  :ellipse  :feret  :integrated-density  :invert-y  
   :kurtosis  :labels  :limit  :max-standards  :mean  :median  :min-max  :mode  :nan-empty-cells  :perimeter  :rect  
   :scientific-notation  :shape-descriptors  :skewness  :slice  :stack-position  :std-dev])

(defn get-image-statistics
  "Return a map of image statistics."
  [^ImagePlus imp & args]
  (let [argmap (apply hash-map args)
        measurement-options
        (apply + 
               (for [[k v] argmap]
                 (cond (= k :add-to-overlay) ij.measure.Measurements/ADD_TO_OVERLAY 
                       (= k :area) ij.measure.Measurements/AREA
                       (= k :area-fraction) ij.measure.Measurements/AREA_FRACTION
                       (= k :center-of-mass) ij.measure.Measurements/CENTER_OF_MASS
                       (= k :centroid) ij.measure.Measurements/CENTROID
                       (= k :circularity) ij.measure.Measurements/CIRCULARITY
                       (= k :ellipse) ij.measure.Measurements/ELLIPSE
                       (= k :feret) ij.measure.Measurements/FERET
                       (= k :integrated-density) ij.measure.Measurements/INTEGRATED_DENSITY
                       (= k :invert-y) ij.measure.Measurements/INVERT_Y
                       (= k :kurtosis) ij.measure.Measurements/KURTOSIS
                       (= k :labels) ij.measure.Measurements/LABELS
                       (= k :limit) ij.measure.Measurements/LIMIT
                       (= k :max-standards) ij.measure.Measurements/MAX_STANDARDS
                       (= k :mean) ij.measure.Measurements/MEAN
                       (= k :median) ij.measure.Measurements/MEDIAN
                       (= k :min-max) ij.measure.Measurements/MIN_MAX
                       (= k :mode) ij.measure.Measurements/MODE
                       ;(= k :nan-empty-cells) ij.measure.Measurements/NaN_EMPTY_CELLS
                       (= k :perimeter) ij.measure.Measurements/PERIMETER
                       (= k :rect) ij.measure.Measurements/RECT
                       (= k :scientific-notation) ij.measure.Measurements/SCIENTIFIC_NOTATION
                       (= k :shape-descriptors) ij.measure.Measurements/SHAPE_DESCRIPTORS
                       (= k :skewness) ij.measure.Measurements/SKEWNESS
                       (= k :slice) ij.measure.Measurements/SLICE
                       (= k :stack-position) ij.measure.Measurements/STACK_POSITION
                       (= k :std-dev) ij.measure.Measurements/STD_DEV)))
        ^ij.process.ImageStatistics stats (ij.process.ImageStatistics/getStatistics (.getProcessor imp) measurement-options (.getCalibration imp))]
    {:angle (.angle stats)
     :area (.area stats)
     :area-fraction (.areaFraction stats)
     :bin-size (.binSize stats)
     :perimeter (when (.getRoi imp) (.getLength ^ij.gui.Roi (.getRoi imp))) 
     :circularity (* 4.0 java.lang.Math/PI (/ (.area stats) (apply * (repeat 2 (when (.getRoi imp) (.getLength ^ij.gui.Roi (.getRoi imp))))))) 
     ;:cal (.cal stats); should probably expand calibration
     :dmode (.dmode stats)
     ;:height (.height stats)
     :hist-max (.histMax stats)
     :hist-min (.histMin stats)
     :histogram (seq (.histogram stats))
     :histogram-16bit (seq (.histogram16 stats))
     :hist-y-max (.histYMax stats)
     :kurtosis (.kurtosis stats)
     ;:long-histogram (seq (.longHistogram stats))
     :long-pixel-count (.longPixelCount stats)
     ;:lower-threshold (.lowerThreshold stats)
     :major (.major stats)
     :max (.max stats)
     :max-count (.maxCount stats)
     :mean (.mean stats)
     :median (.median stats)
     :min (.min stats)
     :minor (.minor stats)
     :mode (.mode stats)
     :n-bins (.nBins stats)
     :pixel-count (.pixelCount stats)
     :roi-height (.roiHeight stats)
     :roi-width (.roiWidth stats)
     :roi-x (.roiX stats)
     :roi-y (.roiY stats)
     :skewness (.skewness stats)
     ;:stack-statistics? (.stackStatistics stats)
     :std-dev (.stdDev stats)
     :uncalibrated-mean (.umean stats)
     ;:upper-threshold (.upperThreshold stats)
     ;:width (.width stats)
     :x-center-of-mass (.xCenterOfMass stats)
     :x-centroid (.xCentroid stats)
     :y-center-of-mass (.yCenterOfMass stats)
     :y-centroid (.yCentroid stats)
     ;:pixel-height (.ph stats)
     ;:pixel-width (.pw stats)
     }))   
