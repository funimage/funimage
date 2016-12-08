(ns funimage.test.imagej.ops
  (:use [funimage imp conversion]        
        [clojure.test])
  (:require [funimage.img :as img]
            [funimage.img.cursor :as cursor]
            [funimage.imagej :as ij]
            [funimage.imagej.ops :as ops]))

#_(let [fi (net.imglib2.FinalInterval. (long-array [10 10]))
       tpe (net.imglib2.type.numeric.real.DoubleType.)
       img1 (ops/create-img-CreateImgFromDimsAndType fi tpe)]
   (show-imp (img->imp (ops/math-add-ConstantToArrayImage-AddInt img1 17))))

#_(let [cat (imp->img (open-imp "/Users/kharrington/git/funimage/black_cat.tif"))
       hat (imp->img (open-imp "/Users/kharrington/git/funimage/witch_hat_small.tif"))
       shape (net.imglib2.algorithm.neighborhood.RectangleShape. 3 false)
       adder (fn [img1 img2] (imp-flatten (add-overlay-image (copy-imp (img->imp img1)) (img->imp img2) 110 50 100 true)))]
   (show-imp (tile-imps
               (map (fn [func] (adder cat (func (img/copy hat) shape)))
                    [ops/morphology-dilate-DefaultDilate ops/morphology-erode-DefaultErode]))))

(deftest test-ops
  (let [img (ops/create-img-CreateImgFromDimsAndType (net.imglib2.FinalInterval. (long-array [10 10])) (net.imglib2.type.numeric.real.DoubleType.))]    
    (is img)))

#_(do 
   (ij/show-ui)
   (let [cat (ij/open-img "/Users/kharrington/git/funimage/black_cat.tif")
         hat (ij/open-img "/Users/kharrington/git/funimage/witch_hat_small.tif")
         shape (net.imglib2.algorithm.neighborhood.RectangleShape. 3 false)
         adder (fn [img1 img2] (img/replace-subimg-with-opacity cat hat [110 50] 0))]
     (ij/show (imp->img
                (tile-imps
                  (map (fn [func] (img->imp (adder cat (func (img/copy hat) shape))))
                       [ops/morphology-dilate-DefaultDilate ops/morphology-erode-DefaultErode]))))))
