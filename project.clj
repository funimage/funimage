(defproject funimage/funimage "0.1.64"
  :description "Functional Image Processing with ImageJ/FIJI"
  :url "https://github.com/funimage/funimage"
  :license {:name "Apache v2.0"
            :url "https://github.com/funimage/funimage/LICENSE"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [seesaw "1.4.4"]
                 [me.raynes/fs "1.4.6"]
                 
                 [net.imagej/ij "1.49s"]
                 [sc.fiji/imagescience "2.5.0"]
                 [net.imglib2/imglib2 "2.2.1"]
                 [net.imglib2/imglib2-algorithm-gpl "0.1.4"]
                 [net.imglib2/imglib2-algorithm-fft "0.1.2"]
                 [net.imglib2/imglib2-algorithm "0.2.1"]
                 [net.imglib2/imglib2-ij "2.0.0-beta-28"]
                 [net.imglib2/imglib2-ops "2.0.0-beta-26"]

                 [ome/bioformats_package "5.1.1"]
                 
                 [java3d/j3d-core "1.3.1"]
                 [sc.fiji/AnalyzeSkeleton_ "2.0.4"]
                 
                 [sc.fiji/Auto_Threshold "1.16.0"]
                 [sc.fiji/VIB_ "2.0.3"]
                 [com.github.wendykierp/JTransforms "3.0"]]
  :java-source-paths ["java"]
  :repositories [["imagej" "http://maven.imagej.net/content/groups/hosted/"]
                 ["imagej-releases" "http://maven.imagej.net/content/repositories/releases/"]
                 ["ome maven" "http://artifacts.openmicroscopy.org/artifactory/maven/"]
                 ["imagej-snapshots" "http://maven.imagej.net/content/repositories/snapshots/"]]                 
  :jvm-opts ["-Xmx32g" "-server"] 
  :javac-options ["-target" "1.6" "-source" "1.6"]
  )
