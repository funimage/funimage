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
           [net.imglib2.view Views IntervalView]
           [net.imglib2 Cursor RandomAccess RandomAccessibleInterval Interval]))
    
(defn get-val
  "Get the value of a numeric (RealType) cursor."
  [^Cursor cur]
  (.get ^net.imglib2.type.numeric.RealType (.get cur)))

(defn set-val
  "Get the value of a numeric (RealType) cursor."
  [^Cursor cur val]
  (.set ^net.imglib2.type.numeric.RealType (.get cur) val))

(defn set-byte-val
  "Get the value of a numeric (RealType) cursor."
  [^Cursor cur ^long val]
  (.setInteger ^net.imglib2.type.numeric.integer.GenericByteType (.get cur) val))

(defn inc
  "Increment the value at a cursor."
  [^Cursor cur]
  (.inc ^net.imglib2.type.numeric.RealType (.get cur)))

(defn dec
  "Decrement the value at a cursor."
  [^Cursor cur]
  (.dec ^net.imglib2.type.numeric.RealType (.get cur)))

(defn set-one
  "Set a cursor's value to one."
  [^Cursor cur]
  (.setOne ^net.imglib2.type.numeric.RealType (.get cur)))

(defn set-zero
  "Set a cursor's value to zero."
  [^Cursor cur]
  (.setZero ^net.imglib2.type.numeric.RealType (.get cur)))

(defn copy
  "Copy one cursor to another."  
  [^Cursor cur1 ^Cursor cur2]
  (.set ^net.imglib2.type.numeric.RealType (.get cur1) (.get cur2)))

(defn add
  "Add 2 cursors together."
  [^Cursor cur1 ^Cursor cur2]
  (.add 
    ^net.imglib2.type.numeric.RealType (.get cur1)
    ^net.imglib2.type.numeric.RealType (.get cur2)))

(defn mul
  "Multiply 2 cursors together."
  [^Cursor cur1 ^Cursor cur2]
  (.mul 
    ^net.imglib2.type.numeric.RealType (.get cur1)
    ^net.imglib2.type.numeric.RealType (.get cur2)))

(defn sub
  "Subtract 2 cursors together."
  [^Cursor cur1 ^Cursor cur2]
  (.sub
    ^net.imglib2.type.numeric.RealType (.get cur1)
    ^net.imglib2.type.numeric.RealType (.get cur2)))

(defn div
  "Divide one cursor by another."  
  [^Cursor cur1 ^Cursor cur2]
  (.div
    ^net.imglib2.type.numeric.RealType (.get cur1)
    ^net.imglib2.type.numeric.RealType (.get cur2)))

(defn sum-neighborhood
  "Sum a neighborhood"
  [nbrhood]
  (loop [sum 0
         cur ^net.imglib2.Cursor (.cursor nbrhood)]
    (if (.hasNext cur)
      (do (.fwd cur)
        (recur (+ sum (.get ^net.imglib2.type.numeric.integer.UnsignedByteType (.get cur)))
              cur))
      sum)))


; Consider a macro that would convert from the usual math functions to the cursor math functions
