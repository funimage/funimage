(ns funimage.test.imp.statistics
  (:use [funimage imp]
        [funimage.imp  statistics]
        [clojure.test]))

(deftest test-empty-image
  (let [imp1 (create-image :width 10 :height 10)]
    (is (zero? (:mean (get-image-statistics imp1))))))
