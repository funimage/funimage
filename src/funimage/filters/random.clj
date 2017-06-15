(ns funimage.filters.random
  (:use [funimage imp project utils]
         [funimage.imp calculator roi]
         [funimage.segmentation utils]
         [funimage.filters utils]))


(defn random-filter
  "Returns a random filter and dimensions given the dimension bounds, 
   possible range of element values and the square flag"
  ([] (random-filter 8 2 false))
  ([dim-bounds num-bounds] (random-filter dim-bounds num-bounds false))
  ([dim-bounds num-bounds square?]
    (let [width (make-odd (rand-int dim-bounds));should use clj-random 
          height (if (true? square?)
                   width
                   (make-odd (rand-int dim-bounds)))
          filter (repeatedly (* width height) #(random-negate (rand num-bounds)))]
    [(float-array filter) width height])))