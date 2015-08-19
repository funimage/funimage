(ns funimage.test.imp
  (:use [funimage imp]
        [clojure.test]))

(deftest test-create-image
  (let [imp (create-imp :width 10 :height 10)]
    (is imp)))


