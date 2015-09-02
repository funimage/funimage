(ns funimage.img.cursor
  (:require [clojure.string :as string]
            [clojure.java.io :as io])
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
           [net.imglib2 Cursor RandomAccess RandomAccessibleInterval Interval]))
    
(defn cursor-val
  "Get the value of a numeric (RealType) cursor."
  [^Cursor cur]
  (.get ^net.imglib2.type.numeric.RealType (.get cur)))

(defn cursor-inc
  "Increment the value at a cursor."
  [^Cursor cur]
  (.inc ^net.imglib2.type.numeric.RealType (.get cur)))

(defn cursor-dec
  "Decrement the value at a cursor."
  [^Cursor cur]
  (.dec ^net.imglib2.type.numeric.RealType (.get cur)))

(defn cursor-set-one
  "Set a cursor's value to one."
  [^Cursor cur]
  (.setOne ^net.imglib2.type.numeric.RealType (.get cur)))

(defn cursor-set-zero
  "Set a cursor's value to zero."
  [^Cursor cur]
  (.setZero ^net.imglib2.type.numeric.RealType (.get cur)))

(defn cursor-add
  "Add 2 cursors together."
  [^Cursor cur1 ^Cursor cur2]
  (.add 
    ^net.imglib2.type.numeric.RealType (.get cur1)
    ^net.imglib2.type.numeric.RealType (.get cur2)))

(defn cursor-mul
  "Multiply 2 cursors together."
  [^Cursor cur1 ^Cursor cur2]
  (.mul 
    ^net.imglib2.type.numeric.RealType (.get cur1)
    ^net.imglib2.type.numeric.RealType (.get cur2)))

(defn cursor-sub
  "Subtract 2 cursors together."
  [^Cursor cur1 ^Cursor cur2]
  (.sub
    ^net.imglib2.type.numeric.RealType (.get cur1)
    ^net.imglib2.type.numeric.RealType (.get cur2)))

(defn cursor-div
  "Divide one cursor by another."  
  [^Cursor cur1 ^Cursor cur2]
  (.div
    ^net.imglib2.type.numeric.RealType (.get cur1)
    ^net.imglib2.type.numeric.RealType (.get cur2)))

; Consider a macro that would convert from the usual math functions to the cursor math functions
