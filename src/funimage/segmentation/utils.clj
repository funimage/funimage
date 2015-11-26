(ns funimage.segmentation.utils 
 (:require [clojure.string :as string])
 (:import [ij.measure ResultsTable]          
          ))

(defn get-results-table
  "Return the current results table."
  []
  ^ResultsTable (ResultsTable/getResultsTable)) 

(defn results-table-to-map
  "Convert a results table into a hash map."
  [rt]
  (let [;rt (get-results-table)
        headings (.getHeadings rt)]
    (apply merge
           (doall (for [k (range (.getCounter rt))]
                    {k 
                     (apply hash-map
                            (flatten (for [heading headings]
                                       [(keyword heading) (.getValue rt heading k)])))})))))



