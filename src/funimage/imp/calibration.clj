(ns funimage.imp.calibration  
  (:import [ij.measure Calibration]))

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

  
