(ns funimage.filters  
  (:require [clojure.string :as string])
  (:import [net.imglib2.img ImagePlusAdapter Img]
           [net.imglib2.img.display.imagej ImageJFunctions]
           [net.imglib2.type NativeType]
           [net.imglib2.type.numeric NumericType]
           [net.imglib2.type.numeric.real FloatType]
           [net.imglib2.algorithm.gauss3 Gauss3]
           [net.imglib2.algorithm.dog DifferenceOfGaussian]
           [net.imglib2.algorithm.fft2 FFTConvolution]
           [net.imglib2.view Views]
           [net.imglib2 Cursor]
           [java.util.concurrent Executors])
  #_(:use [funimage basic]))

(defn gaussian-3d
  "Perform a 3D guassian convolution."
  [img sx sy sz]
  (let [sigma (double-array [sx sy sz])
        infiniteImg (Views/extendValue img (FloatType.))]
    (Gauss3/gauss sigma infiniteImg img )
    img))

(defn dog-3d
  "Perform a 3D DoG."
  [img sx1 sy1 sz1 sx2 sy2 sz2]
  (let [sigma1 (double-array [sx1 sy1 sz1])
        sigma2 (double-array [sx2 sy2 sz2])
        infiniteImg (Views/extendValue img (FloatType.))
        es (Executors/newFixedThreadPool 1000)]
    (DifferenceOfGaussian/DoG sigma1 sigma2 infiniteImg img es)
    img))

(defn convolve-3d
  "Perform a 3D convolution with an arbitrary kernel."
  [img kernel]
  (FFTConvolution. (copy-image img) kernel))

(defn convolve
  "Perform a FFT convolution with an arbitrary kernel."
  [img kernel]
  (FFTConvolution. (copy-image img) kernel))
  