(ns funimage.test.imp
  (:use [funimage imp]
        [clojure.test]))

(deftest test-create-image
  (let [imp (create-imp :width 10 :height 10)]
    (is imp)))

#_(let [filename "/Volumes/Amnes/Claudia/ERG_Dll4_VEcad_IsoB4/07-06-2015/Ctl_P5_IsoB4594_ERG405_Dll4647_VEcad488b_Stitch.czi"
     imp (open-imp filename)
     channels (split-channels imp)]
   (println (get-stack-depth imp))
   (println "Channels:" (count channels))
   (doseq [ch channels]
     (show-imp ch)
     (println ch (get-stack-depth ch))))

