(ns funimage.test.img
  (:use [funimage imp img conversion]
        [funimage.img cursor]
        [clojure.test]))

#_(deftest test-create-image
   (let [imp (create-imp :width 10 :height 10)]
     (is imp)))

(deftest test-img-add
  (let [w 5 h 5
        img1 (first (walk-imgs cursor-set-one
                               (imp->img (create-imp :width w :height h))))
        img2 (first (walk-imgs cursor-set-one
                               (imp->img (create-imp :width w :height h))))
        img-res (first (walk-imgs cursor-add
                                  img1 img2))
        img-sum (sum-img img-res)]
    (is img-sum (* 2 w h))))
    
