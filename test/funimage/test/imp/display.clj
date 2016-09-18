(ns funimage.test.imp.display
  (:use [funimage imp]
        [clojure.test]))

; Disabling for Travis
#_(deftest test-show-imp
   (let [imp1 (open-imp "http://mirror.imagej.net/images/blobs.gif")]
     (show-imp imp1)
     (is imp1)))
        
