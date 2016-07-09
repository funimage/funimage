(ns funimage.test.imp
  (:use [funimage imp]
        [clojure.test]))

(deftest test-create-image
  (let [imp (create-imp :width 10 :height 10)]
    (is imp)))

(deftest test-open-imp-jpg
  (let [imp (open-imp "http://mirror.imagej.net/ij/images/Cell_Colony.jpg")]
    (and (is imp)
         (is (= 112 (get-pixel-unsafe imp 300 300))))))

(deftest test-open-imp-png
  (let [imp (open-imp "http://mirror.imagej.net/ij/images/DentalRadiograph.png")]
    (and (is imp)
         (is (= 82 (get-pixel-unsafe imp 1200 700))))))
