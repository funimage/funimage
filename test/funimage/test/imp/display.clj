(ns funimage.test.imp.display
  (:use [funimage imp]
        [clojure.test]))

(deftest test-show-imp
  (let [imp1 (open-imp "http://mirror.imagej.net/images/blobs.gif")]
    (show-imp imp1)
    (is imp1)))
        
