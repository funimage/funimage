(ns funimage.utils  
  (:require [clojure.string :as string])
  (:import [java.io File]
           [ij IJ ImagePlus]
           [loci.plugins BF]
           [ij.io FileSaver]
           [javax.media.j3d Transform3D]
           [javax.vecmath Vector3f Point3f Quat4f]
           [ij.gui NewImage Toolbar]
           [ij.process ImageProcessor ByteProcessor ImageStatistics]
           
           [java.util.concurrent Executors]
           [javax.swing JFrame JMenu JMenuBar JMenuItem]
           [java.awt Canvas Graphics]
           
           [net.imglib2.img ImagePlusAdapter Img]
           [net.imglib2.img.display.imagej ImageJFunctions]
           [net.imglib2.type NativeType]
           [net.imglib2.type.numeric NumericType]
           [net.imglib2.type.numeric.real FloatType]
           [net.imglib2.algorithm.gauss3 Gauss3]
           [net.imglib2.algorithm.dog DifferenceOfGaussian]
           [net.imglib2.view Views]
           [net.imglib2 Cursor]
           
           [ij.gui HistogramWindow]
           ))

(defn get-image-histogram
  "Display and return the histogram of an image."
  [img]
  (let [hw (HistogramWindow. (.getImagePlus img))]
    (.show hw)
    hw))

(defn imagej-toolbar
  "Make an ImageJ toolbar. (Needs to be connected to images)"
  []
  (let [f (JFrame.)]
    (.setSize f 580 80)
    (let [c (Canvas.)]
      (.add f c)
      (let [tool (Toolbar.)
            g (.getGraphics tool)]
        (.installStartupTools tool)
        (let [menubar (JMenuBar.)
              menu (JMenu. "Menu")
              toolbar (JMenuItem. "Toobar")]
          (.setVisible tool true)
          (.revalidate tool)
          (.add f tool)
          (.setVisible f true))
        tool))))

(defn listen-with-toolbar
  "Register a toolbar to listen to an imageplus."
  [^Toolbar tb ^ImagePlus imp]
  (when (.getCanvas imp)
    (.addMouseListener (.getCanvas imp) tb)))

(defn start-imagej
  "Start a proper ImageJ session."
  ([] (start-imagej nil))
  ([plugins-dir]
    (when-not (nil? plugins-dir)
      (System/setProperty "plugins.dir" plugins-dir)); should we setup more?    
    (let [imagej (ij.ImageJ.)]      
      (ij.Prefs/load imagej nil)          
      (ij.Menus/updateImageJMenus)
      )))


