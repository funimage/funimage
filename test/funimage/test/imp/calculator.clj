(ns funimage.test.imp.calculator
  (:use [funimage imp]
        [funimage.imp calculator statistics]
        [clojure.test]))

(deftest test-create-image
  (let [imp1 (IJ/openImage "http://imagej.nih.gov/ij/images/boats.gif")        
        imp2 (imp-sub imp1 imp1)]
    (is (zero? (imp-sum imp2)))))