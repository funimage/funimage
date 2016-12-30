(ns funimage.test.imagej.ops
  (:use [funimage imp conversion]        
        [clojure.test])
  (:require [funimage.img :as img]
            [funimage.img.cursor :as cursor]
            [funimage.imagej :as ij]
            [funimage.imagej.ops :as ops]
            ;[funimage.imagej.ops.create :as create]
            ;[funimage.imagej.ops.morphology :as morphology]
            [funimage.img.utils :as img-utils]))

(deftest test-ops
  (let [img (funimage.imagej.ops.create/img (net.imglib2.FinalInterval. (long-array [10 10])) (net.imglib2.type.numeric.real.DoubleType.))]    
    (is img)))

#_(do 
   (ij/show-ui)
   (let [cat (ij/open-img "/Users/kharrington/git/funimage/black_cat.tif")
         hat (ij/open-img "/Users/kharrington/git/funimage/witch_hat_small.tif")
         adder (fn [img1 img2] (img/replace-subimg-with-opacity img1 img2 [110 50] 0))]
     (ij/show (img-utils/tile-imgs
                (map (fn [func] (adder (img/copy cat) (func (img/copy hat))))
                     (mapcat (fn [r]
                               [#(funimage.imagej.ops.morphology/dilate % (net.imglib2.algorithm.neighborhood.RectangleShape. r true))
                                #(funimage.imagej.ops.morphology/erode % (net.imglib2.algorithm.neighborhood.RectangleShape. r true))])
                             (range 3 7)))))))
