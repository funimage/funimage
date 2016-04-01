(ns funimage.test.imp
  (:use [funimage imp]
        [clojure.test]))

(deftest test-create-image
  (let [imp (create-imp :width 10 :height 10)]
    (is imp)))

(deftest test-open-imp
  (let [imp (open-imp "http://imagej.nih.gov/ij/images/Cell_Colony.jpg")]
    (and (is imp)
         (is (= 112 (get-pixel-unsafe imp 300 300))))))
