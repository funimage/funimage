(ns funimage.imp.luts
  (:use [funimage imp project utils]
        [funimage.imp calculator roi]
        [funimage.segmentation utils imp])
  (:require [clojure.string :as string])
  (:import [ij IJ ImagePlus ImageStack]
           [ij.measure ResultsTable]
           [ij.gui WaitForUserDialog GenericDialog Wand]
           [ij.plugin ImageCalculator Duplicator]
           [ij.process ImageConverter LUT]))

(defonce luts (atom {}))

(defn initialize-luts
  "Initialize LUTs from a directory. (This should probably be called from an initialization function that accesses a FIJI.app"
  [path]
  (when path
	   (let [f (java.io.File. path)]
      (when (and (.exists f) (.isDirectory f))
        (let [contents (.list f)]
          (doseq [filename contents]
            (when (.endsWith filename ".lut")
              (let [lut-name (.substring filename 0 (- (.length filename) 4))]
                (swap! luts assoc lut-name 
                       {:filename (str path java.io.File/separator filename)
                        :color-model (ij.plugin.LutLoader/open (str path java.io.File/separator filename))})))))))))

(defn show-color-model
  "Show a color model"
  [cm]
  (let [height 10
        width 256
        cmap-array (int-array 256)
        rarray (byte-array 256)
        garray (byte-array 256)
        barray (byte-array 256)
        imp (create-imp :width width :height height :type "RGB")
        proc (.getProcessor imp)]
    (.getRGBs cm cmap-array)    
    (.getReds cm rarray)        
    (.getGreens cm garray)        
    (.getBlues cm barray)        
    (doall (for [x (range width)
                 y (range height)]
             #_(.putPixel proc x y (int-array [(nth rarray x) (nth garray x) (nth barray x)]))
             #_(put-pixel-int imp x y (int-array [(nth rarray x) (nth garray x) (nth barray x)]))
             (put-pixel-int imp x y (nth cmap-array x))))
    (show-imp imp)))

(defn show-lut
  "Show a LUT."
  [cm]
	(let [width 256
        height 128
        lut (ij.LookUpTable. cm)
        ;int mapSize = lut.getMapSize();
        reds (.getReds lut)
        greens (.getGreens lut)
        blues (.getBlues lut)
        ;isGray = lut.isGrayscale();
        ;java.awt.Image img = IJ.getInstance().createImage(imageWidth, imageHeight);
        imp (create-imp :width width :height height :type "RGB")
        image (.getBufferedImage imp)
        g (.getGraphics image)]
   (.fillRect g 0 0 width height)
   (.setColor g java.awt.Color/black)
   (.drawColorBar lut g 0 0 256 height)
   (.dispose g)
   (show-imp (ij.ImagePlus. "LUT" image))))

(initialize-luts "/Applications/Fiji 2.app/luts")
#_(doseq [[k v] @luts]
   (println k v))
;(show-color-model (:color-model (get @luts "unionjack")))
(show-lut (:color-model (get @luts "unionjack")))

#_(let [tmp (int-array 256)]
   (.getRGBs (:color-model (get @luts "smart")) tmp)
   (seq tmp))

#_(defn get-luts
   "Return a list of all available LUTs."
   []
   ;(ij.IJ/getLuts); This is a menu-based command
   (let [path (ij.IJ/getDirectory "luts")]
     (println path)
     (when path
	      (let [f (java.io.File. path)]
         (when (and (.exists f) (.isDirectory f))
           (let [contents (.list f)]
	 ; 	if (IJ.isLinux()) StringSorter.sort(list);
             (seq contents)))))))
	; 	submenu.addSeparator();
 ; 		for (int i=0; i<list.length; i++) {
 ; 			String name = list[i];
 ; 			if (name.endsWith(".lut")) {
 ; 				name = name.substring(0,name.length()-4);
 ; 				if (name.contains("_") && !name.contains(" "))
 ; 					name = name.replace("_", " ");
 ; 				MenuItem item = new MenuItem(name);
	; 			submenu.add(item);
	; 			item.addActionListener(ij);
	; 			nPlugins++;
	; 		}
	; 	}
  


(let [imp (open-imp "DrosophilaWing.tif")]
  (show-imp imp)
  #_(println (get-luts)))


; Draw color bar
