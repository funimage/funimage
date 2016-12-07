(ns funimage.test.imagej.ops
  (:use [funimage imp conversion]        
        [clojure.test])
  (:require [funimage.img :as img]
            [funimage.img.cursor :as cursor]
            [funimage.imagej.ops :as ops]))

#_(let [fi (net.imglib2.FinalInterval. (long-array [10 10]))
       tpe (net.imglib2.type.numeric.real.DoubleType.)
       img1 (ops/create-img-CreateImgFromDimsAndType fi tpe)]
   (show-imp (img->imp img1)))
