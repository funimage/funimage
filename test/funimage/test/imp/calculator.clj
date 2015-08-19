(ns funimage.test.imp.calculator
  (:use [funimage imp]
        [funimage.imp calculator statistics]
        [clojure.test]))

(deftest test-create-image
  (let [imp1 (ij.IJ/openImage "http://imagej.nih.gov/ij/images/boats.gif")        
        imp2 (imp-subtract imp1 imp1)]
    (is (zero? (:mean (get-image-statistics imp2))))))