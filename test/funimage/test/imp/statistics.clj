(ns funimage.test.imp.statistics)

(do 
  (def filename "/Users/kyle/git/funimage/hello-communist-kitty_bw.tif")
  (use 'funimage.imp) 
  (def imp (open-imp filename))
  
  (show-imp imp)
  (println filename)
  (def stats (get-image-statistics imp :std-dev true :skewness true))
  (println (:skewness stats))
  (println stats)
  )
