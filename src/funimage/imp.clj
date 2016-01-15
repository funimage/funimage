;; This is the namespace for ImagePlus utilities (see img for imglib2 img's)
(ns funimage.imp
  (:require [clojure.string :as string]
            [clojure.java.io :as io])
  (:import [java.io File]
           [ij IJ ImagePlus ImageStack]
           [loci.plugins BF]
           [ij.io FileSaver]
           [ij.gui NewImage Toolbar Roi]
           [ij.process ImageProcessor ByteProcessor ImageStatistics]
           [ij.measure Calibration]
           [ij.plugin Duplicator]
           
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

(defn get-current-imp
  "Return the currently open ImagePlus."
  []
  ^ImagePlus (IJ/getImage))

(defn open-imp
  "Open an image using bioformats, this returns an array of imageplus's. Return the first, see open-imps to open many imagepluses at once"
  [filename]    
  ^ImagePlus (first (BF/openImagePlus filename)))

#_(defn open-imp
    "Open an image using bioformats, this returns an array of imageplus's. Return the first, see open-imps to open many imagepluses at once. If bioformats messes up, it attempts to open it with the normal imageplus reader"
    [filename]    
    ^ImagePlus (try
                 (first (BF/openImagePlus filename))
                 #_(catch Exception e (str "cannot open " filename " with bioformats, attempting imageplus reader."))
                 (finally (ImagePlus. filename))))

(defn open-imps
  "Open an image using bioformats, this returns an array of imageplus's"
  [filename]    
  (BF/openImagePlus filename))

(defn show-imp
  "Display an image, expects an ImagePlus."
  [^ImagePlus imp]
  ; Don't show if in headless mode
  (.show ^ImagePlus imp)
  imp)

(defn get-width
  "Return the width of an image."
  [imp]
  (.getWidth ^ImagePlus imp))

(defn get-height
  "Return the height of an image."
  [imp]
  (.getHeight ^ImagePlus imp))

(defn get-stack-depth
  "Return the depth of the image stack."
  [imp]
  (.getImageStackSize ^ImagePlus imp))

(defn create-imp
  "Make a new image plus"
  [& {:keys [title type width height depth channels slices frames]
      :or {title "New ImagePlus"
           type "16-bit" ;"8-bit", "16-bit", "32-bit" or "RGB". May also contain "white" , "black" (the default), "ramp", "composite-mode", "color-mode", "grayscale-mode or "label".
           width 512
           height 512
           channels 1
           slices 1
           frames 1}}]
  ^ImagePlus (IJ/createImage title type width height channels slices frames))

(defn save-imp
  "Save an image."
  [imp filename]
  (IJ/save ^ImagePlus imp ^string filename))

(defn save-imp-as-tiff
  "Save an image as a tiff."
  [imp filename]
  (IJ/saveAsTiff ^ImagePlus imp ^string filename))

(defn imp-dimensions
  "Return the dimensions of an image."
  [imp]
  (let [dim (long-array (.numDimensions ^ImagePlus imp))]
    (.dimensions ^ImagePlus imp ^longs dim)
    dim))

(defn set-interpolation-method
  "Set the interpolation method of the current processor of an imageplus."
  [imp method]
  (cond (= method :none) (.setInterpolationMethod (.getProcessor imp) ij.process.ImageProcessor/NONE)
        (= method :bilinear) (.setInterpolationMethod (.getProcessor imp) ij.process.ImageProcessor/BILINEAR)
        (= method :bicubic) (.setInterpolationMethod (.getProcessor imp) ij.process.ImageProcessor/BICUBIC))
  imp)

(defn resize-imp
  "Resize an image."
  [^ImagePlus imp width height]
  (let [stack ^ImageStack (.getStack imp)
        new-stack ^ImageStack (ij.ImageStack. width height)]
     (dotimes [k (.getSize stack)]
       (.addSlice new-stack 
         (.resize ^ImageProcessor (.getProcessor stack (inc k)) width height true)))
     (ImagePlus. (str (.getTitle ^ImagePlus imp) "-resized_" width "x" height) new-stack)
     #_(ImagePlus. (str (.getTitle ^ImagePlus imp) "-resized_" width "x" height)
                  (.resize (.getProcessor imp) width height true)
                  #_(.getScaledInstance (.getImage ^ImagePlus imp) width height java.awt.Image/SCALE_SMOOTH))))

;; ImageProcessor based functions

(defn auto-threshold
  "Automatically determine and threshold the imageplus."
  [^ImagePlus imp]
  (let [stack ^ImageStack (.getStack imp)]
     (dotimes [k (.getSize stack)]       
       (.autoThreshold ^ImageProcessor (.getProcessor stack (inc k)))))
  imp)

(defn blur-gaussian
   "Gaussian blur on the currently imageplus/ROI."
   [^ImagePlus imp sigma]
   (let [stack ^ImageStack (.getStack imp)]
     (dotimes [k (.getSize stack)]       
       (.blurGaussian ^ImageProcessor (.getProcessor stack (inc k)) sigma)))
   imp)

(defn convolve
  "Convolve a kernel (kernel is a float array) with an imageplus."
  [^ImagePlus imp kernel kernel-width kernel-height]
  (let [stack ^ImageStack (.getStack imp)]
     (dotimes [k (.getSize stack)]       
       (.convolve ^ImageProcessor (.getProcessor stack (inc k)) ^floats kernel ^int kernel-width ^int kernel-height)))
  imp)

(defn convolve-stack
  "Convolve a kernel with the whole stack."
  [^ImagePlus imp kernel kernel-width kernel-height]
  (let [stack ^ImageStack (.getImageStack imp)
        outstack ^ImageStack (ImageStack. (.getWidth stack) (.getHeight stack))]
    (dotimes [k (.getSize stack)]
      (.convolve ^ImageProcessor (.getProcessor stack (inc k)) ^floats kernel ^int kernel-width ^int kernel-height)
      (.addSlice outstack ^ImageProcessor (.getProcessor stack (inc k))))
    (ImagePlus. (.getTitle imp) outstack)))

(defn crop
  "Crop using the currently active ROI."
  [^ImagePlus imp]  
  (ImagePlus. (.getTitle imp) (.crop ^ImageProcessor (.getProcessor imp))))

(defn crop-stack
  "Crop the whole stack using the currently active ROI (bounding rectangle if non rectangle roi)."
  [^ImagePlus imp]  
  (let [rect (.getBounds (.getRoi imp))]
    (ImagePlus. (.getTitle imp) (.crop ^ImageStack (.getImageStack imp) (.x rect) (.y rect) 0 (.width rect) (.height rect) (get-stack-depth imp)))))
; crop-stack could have a version with substack indices as well

(defn dilate
   "Dilate the currently imageplus/ROI."
   [^ImagePlus imp]
   (let [stack ^ImageStack (.getStack imp)]
     (dotimes [k (.getSize stack)]       
       (.dilate ^ImageProcessor (.getProcessor stack (inc k)))))
   imp)

(defn draw-line
  "Draw a line on the imageplus from the 2 xy coords."
  [^ImagePlus imp x1 y1 x2 y2]
  (.drawLine ^ImageProcessor (.getProcessor imp) x1 y1 x2 y2)
  imp)

;; More drawing functions can be added drawDot drawOval drawPixel drawPolygon drawRect drawRoi drawString

(defn erode
   "Erode the currently imageplus/ROI."
   [^ImagePlus imp]
   (let [stack ^ImageStack (.getStack imp)]
     (dotimes [k (.getSize stack)]       
       (.erode ^ImageProcessor (.getProcessor stack (inc k)))))
   imp)

(defn exp
  "Exponential tranform on the imageplus/ROI"
  [^ImagePlus imp]
  (let [stack ^ImageStack (.getStack imp)]
     (dotimes [k (.getSize stack)]       
       (.exp ^ImageProcessor (.getProcessor stack (inc k)))))
  imp)

(defn fill
  "Fill the image/ROI with current fill value. This might not have the desirable ROI behaviors."
  [^ImagePlus imp]
  (let [stack ^ImageStack (.getStack imp)]
     (dotimes [k (.getSize stack)]       
       (.fill ^ImageProcessor (.getProcessor stack (inc k)))))
  imp)

; More fill functions could be added fillOutside fillOval fillPolygon

(defn filter-imp
  "3x3 filter on imageplus, arguments are:
:blur-more, :find-edges, :median-filter, :min, :max"
  [^ImagePlus imp filter-type]
  (let [stack ^ImageStack (.getStack imp)]
     (dotimes [k (.getSize stack)]       
       (cond (= filter-type :blur-more) (.filter ^ImageProcessor (.getProcessor stack (inc k)) ij.process.ImageProcessor/BLUR_MORE)  
             (= filter-type :find-edges) (.filter ^ImageProcessor (.getProcessor stack (inc k)) ij.process.ImageProcessor/FIND_EDGES)  
             (= filter-type :median-filter) (.filter ^ImageProcessor (.getProcessor stack (inc k)) ij.process.ImageProcessor/MEDIAN_FILTER)  
             (= filter-type :min) (.filter ^ImageProcessor (.getProcessor stack (inc k)) ij.process.ImageProcessor/MIN)  
             (= filter-type :max) (.filter ^ImageProcessor (.getProcessor stack (inc k)) ij.process.ImageProcessor/MAX)))
     imp))

(defn find-edges
  "Find edges in the imageplus/roi"
  [^ImagePlus imp]
  (let [stack ^ImageStack (.getStack imp)]
     (dotimes [k (.getSize stack)]       
       (.findEdges ^ImageProcessor (.getProcessor stack (inc k))))
     imp))

(defn flip-horizontal
  "Flip the imageplus horizontally."
  [^ImagePlus imp]
  (let [stack ^ImageStack (.getStack imp)]
     (dotimes [k (.getSize stack)]       
       (.flipHorizontal ^ImageProcessor (.getProcessor stack (inc k)))))
  imp)

(defn flip-vertical
  "Flip the imageplus vertically."
  [^ImagePlus imp]
  (let [stack ^ImageStack (.getStack imp)]
     (dotimes [k (.getSize stack)]       
       (.flipVertical ^ImageProcessor (.getProcessor stack (inc k)))))
  imp)

(defn gamma
  "Gamma correction on the imageplus."
  [^ImagePlus imp val]
  (let [stack ^ImageStack (.getStack imp)]
     (dotimes [k (.getSize stack)]       
       (.gamma ^ImageProcessor (.getProcessor stack (inc k)) val)))
  imp)

(defn get-pixel-unsafe
  "Get pixel without bounds checking (faster)"
  [^ImagePlus imp x y]
  (.get ^ImageProcessor (.getProcessor imp) x y))

(defn get-auto-threshold
  "Get the value that would be used by auto threshold."
  [^ImagePlus imp]
  (.getAutoThreshold ^ImageProcessor (.getProcessor imp)))

(defn get-background-value
  "Return the current background fill value."
  [^ImagePlus imp]
  (.getBackgroundValue ^ImageProcessor (.getProcessor imp)))

(defn get-bit-depth
  "Return the bit depth of an imageplus."
  [^ImagePlus imp]
  (.getBitDepth ^ImageProcessor (.getProcessor imp)))

(defn get-calibration
  "Return the calibration of an imageplus."
  [^ImagePlus imp]
  ^ij.measure.Calibration (.getCalibration imp))

(defn get-pixel-float
  "Return a pixel's value as a float."
  [^ImagePlus imp x y]
  (.getf ^ImageProcessor (.getProcessor imp) x y))

(defn get-num-channels
  "Return the number of channels in an ImagePlus."
  [^ImagePlus imp]
  (.getNChannels imp))

(defn get-num-dimensions
  "Return the number of dimensions in an ImagePlus."
  [^ImagePlus imp]
 (.getNDimensions imp))

(defn get-num-frames
  "Return the number of frames in an ImagePlus."
  [^ImagePlus imp]
  (.getNFrames imp))

(defn get-num-slices
  "Return the number of slices in an ImagePlus."
  [^ImagePlus imp]
  (.getNSlices imp))

(defn get-numeric-property
  "Return a numberic property from the info property string."
  [^ImagePlus imp propkey]
  (.getNumericProperty imp propkey))

(defn get-histogram
  "Return the histogram of the imageplus/ROI."
  [^ImagePlus imp]
  (.getHistogram ^ImageProcessor (.getProcessor imp)))

(defn get-histogram-max
  "Return the maximum value for the image (for float imagepluses)."
  [^ImagePlus imp]
  (.getHistogramMax ^ImageProcessor (.getProcessor imp)))

(defn get-histogram-min
  "Return the minimum value for the image (for float imagepluses)."
  [^ImagePlus imp]
  (.getHistogramMin ^ImageProcessor (.getProcessor imp)))
                 
(defn get-histogram-size
  "Return the number of bins of a histogram (for float imagepluses)."
  [^ImagePlus imp]
  (.getHistogramSize ^ImageProcessor (.getProcessor imp)))

(defn get-line
  "Return an array of pixels along the line within the imageplus."
  [^ImagePlus imp x1 y1 x2 y2]
  (.getLine ^ImageProcessor (.getProcessor imp) x1 y1 x2 y2))

(defn get-max
  "Return the maximum value in an imageplus."
  [^ImagePlus imp]
  (.getMax ^ImageProcessor (.getProcessor imp)))

(defn get-min
  "Return the minimum value in an imageplus."
  [^ImagePlus imp]
  (.getMin ^ImageProcessor (.getProcessor imp)))

(defn get-pixel
  "Return the pixel value at x,y (this does bounds checking and is slower, for a faster op check get-pixel-unsafe)"
  [^ImagePlus imp x y]
  (.getPixel ^ImageProcessor (.getProcessor imp) x y))

(defn get-roi
  "Returns the roi of an imp."
  [^ImagePlus imp]
  ^Roi (.getRoi imp))

#_(defn get-image-statistics
   "Return the image statistics for an imageplus."
   [^ImagePlus imp]
   (.getStatistics ^ImageProcessor (.getProcessor imp)))

(defn invert
  "Invert an imageplus/roi"
  [^ImagePlus imp]  
  (let [stack ^ImageStack (.getStack imp)]
     (dotimes [k (.getSize stack)]       
       (.invert ^ImageProcessor (.getProcessor stack (inc k)))))
   imp)

(defn binary?
  "Is an imageplus binary?"
  [^ImagePlus imp]
  (.isBinary ^ImageProcessor (.getProcessor imp)))

(defn grayscale?
  "Is an imageplus grayscale?"
  [^ImagePlus imp]
  (.isGrayscale ^ImageProcessor (.getProcessor imp)))

(defn median-filter
  "3x3 median filter on an imageplus"
  [^ImagePlus imp]
  (let [stack ^ImageStack (.getStack imp)]
     (dotimes [k (.getSize stack)]       
       (.medianFilter ^ImageProcessor (.getProcessor stack (inc k)))))
  imp)

(defn scalar-multiply
  "Multiply every pixel of an imageplus by a value."
  [^ImagePlus imp val]
  (let [stack ^ImageStack (.getStack imp)]
     (dotimes [k (.getSize stack)]       
       (.multiply ^ImageProcessor (.getProcessor stack (inc k)) val)))
  imp)

(defn add-noise
  "Add noise to an imageplus with gaussian of mean 0 and the supplied std-dev."
  [^ImagePlus imp std-dev]
  (let [stack ^ImageStack (.getStack imp)]
     (dotimes [k (.getSize stack)]       
       (.noise ^ImageProcessor (.getProcessor stack (inc k)) std-dev)))
  imp)

(defn put-pixel-int
  "Put a pixel value at x,y of the imageplus."
  [^ImagePlus imp x y val]
  (.putPixel ^ImageProcessor (.getProcessor imp) (int x) (int y) (int val))
  imp)

(defn put-pixel-int-unsafe
  "Put a pixel value at x,y of an imageplus without bounds checking."
  [^ImagePlus imp x y val]
  (.set ^ImageProcessor (.getProcessor imp) x y val)
  imp)

(defn put-pixel-double
  "Put a pixel value at x,y of the imageplus."
  [^ImagePlus imp x y val]
  (.putPixelValue ^ImageProcessor (.getProcessor imp) x y val)
  imp)

(defn put-pixel-double-unsafe
  "Put a pixel value at x,y of the imageplus."
  [^ImagePlus imp x y val]
  (.setf ^ImageProcessor (.getProcessor imp) x y val)
  imp)

(defn reset-undo-buffer
  "Reset the undo buffer for an imageplus."
  [^ImagePlus imp]
  (let [stack ^ImageStack (.getStack imp)]
     (dotimes [k (.getSize stack)]       
       (.reset ^ImageProcessor (.getProcessor stack (inc k)))))
  imp)

(defn rotate
  "Rotate an imageplus by the supplied angle."
  [^ImagePlus imp angle]
  (let [stack ^ImageStack (.getStack imp)]
     (dotimes [k (.getSize stack)]       
       (.rotate ^ImageProcessor (.getProcessor stack (inc k)) angle)))
  imp)

(defn rotate-left
  "Rotate an imageplus left by 90degrees."
  [^ImagePlus imp]
  (let [stack ^ImageStack (.getStack imp)]
     (dotimes [k (.getSize stack)]       
       (.rotateLeft ^ImageProcessor (.getProcessor stack (inc k)))))
  imp)

(defn rotate-right
  "Rotate an imageplus right by 90degrees."
  [^ImagePlus imp]
  (let [stack ^ImageStack (.getStack imp)]
     (dotimes [k (.getSize stack)]       
       (.rotateRight ^ImageProcessor (.getProcessor stack (inc k)))))
  imp)

(defn set-background-value
  "Set the background fill value of an imageplus."
  [^ImagePlus imp val]
  (let [stack ^ImageStack (.getStack imp)]
     (dotimes [k (.getSize stack)]       
       (.setBackgroundValue ^ImageProcessor (.getProcessor stack (inc k)) val)))
  imp)

(defn set-calibration
  "Set the calibration of an imageplus."
  [^ImagePlus imp ^Calibration cal]
  (.setCalibration imp cal)
  imp)

(defn set-histogram-range
  "Set the min/max of an imageplus' histogram."
  [^ImagePlus imp hmin hmax]
  (.setHistogramRange ^ImageProcessor (.getProcessor imp) hmin hmax)
  imp)

(defn set-histogram-size
  "Set the number of histogram bins for an imageplus."
  [^ImagePlus imp nbins]
  (.setHistogramSize ^ImageProcessor (.getProcessor imp) nbins)
  imp)

(defn set-roi
   "Set the roi of the imageplus to start-x start-y width height."
   [^ImagePlus imp ^Roi roi]
   (.setRoi imp roi)
   imp)

(defn set-fill-value
  "Set the default fill value for an imageplus."
  [^ImagePlus imp val]
  (let [stack ^ImageStack (.getStack imp)]
     (dotimes [k (.getSize stack)]       
       (.setValue ^ImageProcessor (.getProcessor stack (inc k)) val)))
  imp)

(defn sharpen
  "Sharpen an imageplus/roi with 3x3 kernel."
  [^ImagePlus imp]
  (let [stack ^ImageStack (.getStack imp)]
     (dotimes [k (.getSize stack)]       
       (.sharpen ^ImageProcessor (.getProcessor stack (inc k)))))
  imp)

(defn smooth
  "Smooth and image with 3x3 mean."
  [^ImagePlus imp]
  (let [stack ^ImageStack (.getStack imp)]
     (dotimes [k (.getSize stack)]       
       (.smooth ^ImageProcessor (.getProcessor stack (inc k)))))
  imp)

(defn subtract-scalar
  "Subtract a scalar from all pixels of an imageplus."
  [^ImagePlus imp val]
  (let [stack ^ImageStack (.getStack imp)]
     (dotimes [k (.getSize stack)]       
       (.subtract ^ImageProcessor (.getProcessor stack (inc k)) val)))
  imp)

(defn threshold 
  "Threshold an imageplus with a supplied threshold."
  [^ImagePlus imp thresh]
  (.threshold ^ImageProcessor (.getProcessor imp) thresh)
  imp)

(defn threshold-stack 
   "Threshold the whole stack of an imageplus with a supplied threshold."
   [^ImagePlus imp thresh]
   (let [stack ^ImageStack (.getStack imp)]
     (dotimes [k (.getSize stack)]
       (.threshold ^ImageProcessor (.getProcessor stack (inc k)) thresh)))       
   imp)

;; ImageStack

(defn get-voxel
  "Return the voxel value of an imageplus (assuming it is an imagestack)"
  [^ImagePlus imp x y z]
  (.getVoxel ^ImageStack (.getImageStack imp) x y z))

(defn set-voxel
  "Set the value of a voxel of an image plus (assuming it is an imagestack)"
  [^ImagePlus imp x y z val]
  (.setVoxel ^ImageStack (.getImageStack imp) x y z val)
  imp)

(defn convert-to-binary
  "Convert an imageplus to 8 bit using the image stack."
  [^ImagePlus imp]
  (ij.IJ/run imp "Make Binary" "")
  imp
  #_(let [stack ^ImageStack (.getImageStack imp)
         outstack ^ImageStack (ImageStack. (.getWidth stack) (.getHeight stack))]
     (dotimes [k (.getSize stack)]
       (.addSlice outstack
          (.convertToByteProcessor (.getProcessor stack (inc k)))))
     (ImagePlus. (.getTitle imp) outstack)))

(defn convert-to-8bit
  "Convert an imageplus to 8 bit using the image stack."
  [^ImagePlus imp]
  (let [stack ^ImageStack (.getImageStack imp)
        outstack ^ImageStack (ImageStack. (.getWidth stack) (.getHeight stack))]
    (dotimes [k (.getSize stack)]
      (.addSlice outstack
         (.convertToByteProcessor (.getProcessor stack (inc k)))))
    (ImagePlus. (.getTitle imp) outstack)))

(defn convert-to-float
  "Convert an imageplus to 32 bit using the image stack."
  [^ImagePlus imp]
  (let [stack ^ImageStack (.getImageStack imp)
        outstack ^ImageStack (ImageStack. (.getWidth stack) (.getHeight stack))]
    (dotimes [k (.getSize stack)]
      (.addSlice outstack
        (.convertToFloatProcessor (.getProcessor stack (inc k)))))
    (ImagePlus. (.getTitle imp) outstack)))

(defn convert-to-16bit
  "Convert an imageplus to 16 bit using the image stack."
  [^ImagePlus imp]
  (let [stack ^ImageStack (.getImageStack imp)
        outstack ^ImageStack (ImageStack. (.getWidth stack) (.getHeight stack))]
    (dotimes [k (.getSize stack)]
      (.addSlice outstack
         (.convertToShortProcessor (.getProcessor stack (inc k)))))
    (ImagePlus. (.getTitle imp) outstack)))

(defn convert-to-RGB
  "Convert an imageplus to 8 bit using the image stack."
  [^ImagePlus imp]
  (let [stack ^ImageStack (.getImageStack imp)
        outstack ^ImageStack (ImageStack. (.getWidth stack) (.getHeight stack))]
    (dotimes [k (.getSize stack)]
      (.addSlice outstack
        (.convertToRGB (.getProcessor stack (inc k)))))
    (ImagePlus. (.getTitle imp) outstack)))

(defn imp-from-processor
  "Make an image plus from an imageprocessor."
  [^ImageProcessor ip title]
  (ImagePlus. title ip))

(defn imp-from-stack
  "Make an image plus from an imagestack."
  [^ImageStack stack title]
  (ImagePlus. title stack))

(defn get-imp-type #_get-type
  "Return the type of an ImagePlus."
  [^ImagePlus imp]
  (.getType imp))

(defn get-type-string
  "Return the string type."
  [^ImagePlus imp]
  (let [t (get-imp-type imp)]
    (cond (= t ij.ImagePlus/GRAY8) "8-bit"
          (= t ij.ImagePlus/GRAY16) "16-bit"
          (= t ij.ImagePlus/GRAY32) "32-bit"
          (= t ij.ImagePlus/COLOR_256) "RGB"
          (= t ij.ImagePlus/COLOR_RGB) "RGB")))

(defn get-title
  "Return the title of an ImagePlus."
  [^ImagePlus imp]
  (.getTitle imp))

;; Depends on earlier functions

#_(defn copy-imp
   "Return a duplicate of an image."
   [imp]
   (.run (Duplicator.) imp)
   #_(let [new-imp (.duplicate ^ImagePlus imp)]
      (set-calibration new-imp (get-calibration imp))))

(defn copy-imp
  "Return a duplicate of an image."
  [imp]
  (let [^ij.ImageStack stack (.getStack imp)       
        n (.getSize stack)
        stack2 (loop [i 1
                      ^ij.ImageStack stack2 nil]
                 (if (<= i n)
                   (do 
                     (when (.isVirtual stack)
                       (ij.IJ/showStatus (str "Duplicating: " i "/" n)))
                     (let [^ij.process.ImageProcessor ip2 (.getProcessor stack i)
                           stack2 (if (nil? stack2)
                                    ^ij.ImageStack (ij.ImageStack. (.getWidth ip2) (.getHeight ip2) (.getColorModel (.getProcessor imp)))
                                    stack2)]
                       (.addSlice stack2 (.getSliceLabel stack i) (.duplicate ip2))
                       (recur (inc i) stack2)))
                   stack2))]
    (let [imp2 (.createImagePlus imp)
          info (.getProperty imp "Info")
          dim (.getDimensions imp)]
      (.setStack imp2 (str "DUP_" (get-title imp)) stack2)
      (when info (.setProperty imp2 "Info" info))      
      (.setDimensions imp2 (nth dim 2) (nth dim 3) (nth dim 4))
      imp2)))


(defn imp-from-clipboard
  "Return the imageplus in theclipboard."
  []
  (IJ/run "System Clipboard")
  (let [imp (IJ/getImage)]
    (.setTitle imp (str "Pasted_" (System/nanoTime)))
    imp))

(defn create-imp-like
  "Create an ImagePlus like the input but blank."
  [imp]
  (let [new-imp (create-imp :title (get-title imp)
                            :width (get-width imp)
                            :height (get-height imp)
                            :slices (get-stack-depth imp)
                            :type (get-type-string imp))]
    (let [cal (get-calibration imp)
          new-cal (get-calibration new-imp)]
      (set! (.pixelWidth new-cal) (.pixelWidth cal))
      (set! (.pixelHeight new-cal) (.pixelHeight cal))
      (set! (.pixelDepth new-cal) (.pixelDepth cal))
      (.setXUnit new-cal (.getXUnit cal))
      (.setYUnit new-cal (.getYUnit cal))
      (.setZUnit new-cal (.getZUnit cal)))
    new-imp))

(defn split-channels
  "Split channels."
  [^ImagePlus imp]
  (doall
    (for [chan (range (get-num-channels imp))]
      (let [new-stack (ij.ImageStack. (get-width imp) (get-height imp))]
        (dotimes [t (get-num-frames imp)]
          (dotimes [z (get-num-slices imp)]
            (let [n (.getStackIndex imp (inc chan) (inc z) (inc t))]
              (.addSlice new-stack (.getProcessor (.getImageStack imp) n)))))
        (ImagePlus. (str "C" chan "-" (.getTitle imp))
                    new-stack)))))

(defn split-rgb
  "Split an RGB type image."
  [^ij.ImagePlus imp]
  (let [stacks (ij.plugin.ChannelSplitter/splitRGB (.getImageStack imp) false)]
    (doall (for [idx (range (count stacks))]
             (ij.ImagePlus. (str "C" idx "-" (get-title imp))
                            (nth stacks idx))))))

(defn split-stack
  "Split an imagestack."
  [imp]
  (let [stack ^ImageStack (.getImageStack imp)]
    (doall
      (for [k (range (.getSize stack))]
        (ImagePlus. (str "S" k "-" (.getTitle imp))
                    (.getProcessor stack (inc k)))))))

(defn copy-calibration
  "Copy the calibration from imp-a into imp-b"
  [^ij.ImagePlus imp-a ^ij.ImagePlus imp-b]
  (set-calibration imp-b (get-calibration imp-a))
  imp-b)

(defn invert-lut
  "Invert the LUT of all processors in an ImagePlus' ImageStack."
  [^ij.ImagePlus imp]  
  (.invertLut (.getProcessor imp))
  (when (> (.getStackSize imp) 1)
    (.setColorModel (.getStack imp)
      (.getColorModel (.getProcessor imp))))
  (when (.isVisible imp)
		(.updateAndRepaintWindow imp))
  imp)

(defn set-lut
  "Set the lookup table (LUT) for color coding this image."
  [^ImagePlus imp ^ij.process.LUT lut]
  (.setLut imp lut))

(defn create-lut
  "Make a lookup table from an image."
  [^ImagePlus imp]
  ^ij.process.LUT (.createLut imp))
  
(defn set-title
  "Set the title of an imageplus."
  [^ImagePlus imp title]
  (.setTitle imp title)
  imp)

(defn size-filter-stack
  "Size filter an image slice by slice over the whole stack."
  [imp & args]
  (let [argmap (apply hash-map args)
        min-size (or (:min-size argmap) 0)
        max-size (or (:max-size argmap) "Infinity")
        max-size (if (string? max-size) java.lang.Integer/MAX_VALUE max-size)
        label-segments? (or (:label-segments argmap) false); If true, each segment gets a unique intensity value 
        return-mask? true
        rois (atom [])
        stack ^ij.ImageStack (.getImageStack imp)]
    (dotimes [idx (.getSize stack)]
      (let [ip ^ij.process.ImageProcessor (.getProcessor stack (inc idx))
            w ^ij.gui.Wand (ij.gui.Wand. ip)]
        (doall (for [x (range (get-width imp)) y (range (get-height imp))]
                 (when (> (.getPixel ip x y) 0); could throw out contiguous 0s from search as well
                   (.autoOutline w (int x) (int y) (int 1) (int 256))
                   (let [roi ^ij.gui.Roi (ij.gui.PolygonRoi. (.xpoints w) (.ypoints w) (.npoints w) ij.gui.Roi/FREEROI #_ij.gui.Roi/POLYGON)
                         perim (.getLength roi)
                         area (java.lang.Math/pow (/ perim (* 2 java.lang.Math/PI)) 2)
                         r ^java.awt.Rectangle (.getBounds roi)]
                    (when (or  (< area min-size) 
                                (> area max-size))
                      (swap! rois conj roi)
                      (if label-segments?
                        (.setColor ip (count @rois))
                        (.setColor ip 0))
                      (.fillPolygon
                        ip
                        ^java.awt.Polygon (.getPolygon roi)))))))))
    imp))

(defn watershed
  "Take the watershed of the current imageprocessor."
  [^ij.ImagePlus imp]
  (let [edm (ij.plugin.filter.EDM.)]
    (.toWatershed ^ij.process.ImageProcessor (.getProcessor imp))
    imp))

(defn watershed-stack
  "Take the watershed of the current imageprocessor."
  [^ij.ImagePlus imp]
  (let [^ij.plugin.filter.EDM edm (ij.plugin.filter.EDM.)
        stack ^ImageStack (.getStack imp)]
    (dotimes [k (.getSize stack)]       
      (.toWatershed edm ^ImageProcessor (.getProcessor stack (inc k))))
    imp))
  
(defn close-all-images
  "Clear all images from ImageJ, including memory."
  []
  (ij.IJ/run "Close All" ""))
(def clear-all-images close-all-images); deprecated

(defn update-imp
  "Update the display of an imp."
  [^ImagePlus imp]
  (.updateAndRepaintWindow imp))
        
(defn open-image-sequence
  "Open an image sequence."
  [directory]
  (ij.IJ/run "Image Sequence..." (str "open=" directory " sort"))
  (ij.IJ/getImage))  

(defn seq-to-imp
  "Make an image from a sequence. Depth is ignored."
  [width height depth coll]
  (let [imp (create-imp :width width :height height :slices depth :type "8-bit")]
    (dotimes [x width]
      (dotimes [y height]
        ;(dotimes [z depth]
          (set-voxel imp x y 1 (nth coll (+ (* y width) x)))))
    imp))

(defn zconcat-imps
  "Concat a collection of imagepluses along the z-axis. (Might be weird with anything other than 2D images)"
  [imps]
  (let [imp (create-imp :width (get-width (first imps))
                        :height (get-height (first imps))
                        :type (get-type-string (first imps))
                        :slices (count imps))
        out-stack ^ij.ImageStack (.getImageStack imp)]
    (dotimes [k (count imps)]
      (.setProcessor out-stack
        ^ij.process.ImageProcessor (.getProcessor (nth imps k))
        (inc k)))
    imp))

(defn conj-imps
  "Conj an image to another image's stack."
  [target-stack imp]
  (zconcat-imps(conj (vec (split-channels target-stack)) imp))) 

#_(defn imps-to-rgb
   "Convert a sequence of imps (only first 3 or fewer if less supplied) to RGB."
   [imps & args]
   #_(ij.plugin.RGBStackMerge/mergeChannels (into-array ImagePlus (take 3 imps)) false)
   (let [argmap (apply hash-map args)]
     (.mergeHyperstacks (ij.plugin.RGBStackMerge.) (into-array ImagePlus (take 3 imps)) (or (:keep-source argmap) false))))
        
(defn imps-to-rgb
   "Convert a sequence of imps (only first 3 or fewer if less supplied) to RGB."
   [imps & args]
   #_(ij.plugin.RGBStackMerge/mergeChannels (into-array ImagePlus (take 3 imps)) false)
   (let [argmap (apply hash-map args)]
     (ij.plugin.RGBStackMerge/mergeChannels (into-array ImagePlus (take 3 imps)) 
                                            (or (:keep-source argmap) false) )
     #_(.mergeHyperstacks (ij.plugin.RGBStackMerge.) (into-array ImagePlus (take 3 imps)) 
        (or (:keep-source argmap) false))))

; IJ1-style
(defn max-z-projection
  "Return a max Z-projection. This is pretty dangerous because it uses window titles"
  [imp]
  (ij.IJ/run imp "Z Project..." "projection=[Max Intensity]")
  (ij.WindowManager/getImage (str "MAX_" (.getTitle imp))))

#_(defn max-z-projection
    "Return a max Z-projection."
    [^ImagePlus imp]
    (let [start-slice 1
          stop-slice (.getNFrames imp)]
      (cond (.isHyperStack imp)
            ;(do-hyperstack-projection imp start-slice stop-slice)
            nil ; lazier than i should be 
            (= (get-type-string imp) "RGB")
            (apply imps-to-rgb
                   (map max-z-projection (split-rgb imp)))
            :else
            (do-projection imp start-slice stop-slice))))

(defn get-fileinfo
  "Return the fileinfo for an ImagePlus"
  [imp]
  ^ij.io.FileInfo (.getFileInfo imp))

(defn get-filename
  "Return the filename of an imageplus if it exists."
  [imp]
  (.fileName (get-fileinfo imp)))

(defn tile-imps
  "Tile a set of images, currently assumes all are same size as the first one."
  ([imps]
    (when-not (empty? imps)
      (let [width (java.lang.Math/floor (/ (count imps) (java.lang.Math/sqrt (count imps))))
            height (java.lang.Math/ceil (/ (count imps) width))]
        (tile-imps imps width height))))
  ([imps width height]
    (let [imp-width (get-width (first imps))
          imp-height (get-height (first imps))
          tile-imp ^ImagePlus (create-imp :width (* imp-width width) :height (* imp-height height) :type (get-type-string (first imps)))]
      (doall (for [tx (range width)
                   ty (range height)
                   impx (range imp-width)
                   impy (range imp-height)]
               (let [imp-idx (+ tx (* width ty))]; consider getRow, putRow
                 (when (< imp-idx (count imps))
                   (put-pixel-int tile-imp
                              (+ impx (* imp-width tx))
                              (+ impy (* imp-height ty))
                              (get-pixel ^ImagePlus (nth imps imp-idx)
                                         impx impy))))))
      tile-imp)))

(defn imp-flatten
  "Flatten the overlay of an imageplus."
  [^ImagePlus imp]
  (.flatten imp))

(defn add-overlay-image
  "Add an overlay image into an image at a certain position." ;missing a size check
  [^ImagePlus imp ^ImagePlus overlay x y opacity zero-transparent]
  (let [roi (ij.gui.ImageRoi. x y (.getProcessor overlay))]
    (.setName roi "overlay") ;roi.setName(overlay.getShortTitle());		
    (when-not (= opacity 100)
      (.setOpacity roi (/ opacity 100.0)))
    (.setZeroTransparent roi zero-transparent)
    (let [overlay-list (.getOverlay imp)
          overlay-list (if overlay-list overlay-list (ij.gui.Overlay.))]
      (.add overlay-list roi)
			(.setOverlay imp overlay-list)))
  imp); could add undo

(defn autocontrast
  "Auto contrast. This is done the lazy way"
  [imp]
  (ij.IJ/run imp "Enhance Contrast" "saturated=0.35")
  imp)

  
