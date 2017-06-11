(ns funimage.filters.utils
  #_(:use [brevis random])); should use clj-random

(defn make-odd [n] (if (not (odd? n))
                     (+ n 1)
                     n))

(defn random-negate
  [num]
  (let [negate? (rand-int 2)]
    (if (= negate? 0)
      num
      (unchecked-negate num))))