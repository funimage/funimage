(ns funimage.test.img
  (:use [funimage imp conversion]        
        [clojure.test])
  (:require [funimage.img :as img]
            [funimage.img.cursor :as cursor]))

(deftest test-img-add
  (let [w 5 h 5
        img1 (first (img/map cursor/set-one
                               (imp->img (create-imp :width w :height h))))
        img2 (first (img/map cursor/set-one
                               (imp->img (create-imp :width w :height h))))
        img-res (first (img/map cursor/add
                                  img1 img2))
        img-sum (img/sum img-res)]
    (is (= img-sum (* 2 w h)))))

(deftest test-img-inc
  (let [w 5 h 5
        img (first (img/map cursor/inc
                            (imp->img (create-imp :width w :height h))))
        img-sum (img/sum img)]
    (is (= img-sum (* w h)))))
    
