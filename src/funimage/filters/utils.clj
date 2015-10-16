(ns funimage.filters.utils
  (:use [brevis random]))

(defn make-odd [n] (if (not (odd? n))
                     (+ n 1)
                     n))

(defn random-negate
  [num]
  (let [negate? (lrand-int 2)]
    (if (= negate? 0)
      num
      (unchecked-negate num))))