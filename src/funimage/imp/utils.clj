(ns funimage.imp.utils
  (:require [funimage.imp :as imp])
  (:import [ij IJ ImagePlus ImageStack]))

(defn set-foreground
  "Set the ImageJ1 foreground color. Use 0-255 r g b colors"
  [r g b]
  (ij.IJ/setForegroundColor r g b))

(defn set-background
  "Set the ImageJ1 background color. Use 0-255 r g b colors"
  [r g b]
  (ij.IJ/setBackgroundColor r g b))

