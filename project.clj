(defproject funimage/funimage "0.1.88"
  :description "Functional Image Processing with ImageJ/FIJI"
  :url "https://github.com/funimage/funimage"
  :license {:name "Apache v2.0"
            :url "https://github.com/funimage/funimage/LICENSE"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [seesaw "1.4.4"]
                 [me.raynes/fs "1.4.6"]
                 [org.clojure/data.zip "0.1.1"]

                 ; Java libs
                 ;[net.imagej/ij "1.50b"]
                 ;[sc.fiji/imagescience "2.5.0"]
                 
                 ;[net.imglib2/imglib2 "2.4.1"]
                 ;[net.imglib2/imglib2-algorithm-gpl "0.1.5"];[net.imglib2/imglib2-algorithm-gpl "0.1.4"]
                 ;[net.imglib2/imglib2-algorithm-fft "0.1.2"]
                 ;[net.imglib2/imglib2-algorithm "0.3.3"];[net.imglib2/imglib2-algorithm "0.2.1"]
                 ;[net.imglib2/imglib2-ij "2.0.0-beta-31"];[net.imglib2/imglib2-ij "2.0.0-beta-28"]
                 ;[net.imglib2/imglib2-ops "2.0.0-beta-26"]                 
                 ;[net.imglib2/imglib2-io "2.0.0-beta-21"]
                 ;[net.imglib2/imglib2-ui "2.0.0-beta-29"]
                 ;[net.imglib2/imglib2-realtransform "2.0.0-beta-29"]
                 
                 [net.imglib2/imglib2-roi "0.4.3-SNAPSHOT"]
                 
                 ;[io.scif/scifio "0.25.0"]
                 
                 [net.imagej/imagej "2.0.0-rc-43" :exclusions [com.github.jnr/jffi com.github.jnr/jnr-x86asm]]
                 ;[net.imagej/imagej-ops "0.18.1-SNAPSHOTkh"]; currently needed for funimage.img.skeleton
                 ;[net.imagej/imagej-ops "0.23.0"]
                 ;[local/imagej-ops "0.24.2-SNAPSHOT"]
                 ;[net.imagej/imagej-ops "0.24.1"]
                 ;[net.imagej/imagej-ops "0.24.2-SNAPSHOT"]
                 ;[net.imagej/imagej-legacy "0.17.3"]
                 ;[org.scijava/scijava-common "2.46.0"]
                 
                 [ome/bioformats_package "5.1.1"]
                 
                 ;[java3d/j3d-core "1.3.1"]
                 
                 ;[sc.fiji/Skeletonize3D_ "1.0.1"]
                 ;[sc.fiji/AnalyzeSkeleton_ "2.0.4"]
                 
                 [sc.fiji/Auto_Threshold "1.16.0"]
                 ;[sc.fiji/VIB_ "2.0.3"]
                 ;[com.github.wendykierp/JTransforms "3.0"]
                 ]
  :java-source-paths ["java"]
  :repositories [["imagej" "http://maven.imagej.net/content/groups/hosted/"]
                 ["imagej-releases" "http://maven.imagej.net/content/repositories/releases/"]
                 ["ome maven" "http://artifacts.openmicroscopy.org/artifactory/maven/"]
                 ["imagej-snapshots" "http://maven.imagej.net/content/repositories/snapshots/"]]                 
  :jvm-opts ["-Xmx32g" "-server"
             ;"-javaagent:/Users/kyle/.m2/repository/net/imagej/ij1-patcher/0.12.3/ij1-patcher-0.12.3.jar=init"
             #_"-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=localhost:8000"] 
  ;:javac-options ["-target" "1.6" "-source" "1.6"]
  )
