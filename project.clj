(defproject funimage "0.1.60"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]

                 [net.imagej/ij "1.49s"]
                 [sc.fiji/imagescience "2.5.0"]
                 [net.imglib2/imglib2 "2.2.1"]
                 [net.imglib2/imglib2-algorithm-gpl "0.1.4"]
                 [net.imglib2/imglib2-algorithm-fft "0.1.2"]
                 [net.imglib2/imglib2-algorithm "0.2.1"]
                 [net.imglib2/imglib2-ij "2.0.0-beta-28"]
                 [net.imglib2/imglib2-ops "2.0.0-SNAPSHOT"]

                 [ome/bioformats_package "5.1.1"]
                 [seesaw "1.4.4"]
                 [java3d/j3d-core "1.3.1"]
                 [sc.fiji/AnalyzeSkeleton_ "2.0.3-SNAPSHOT"];temp
                 
                 [sc.fiji/Auto_Threshold "1.16.0"]
                 [sc.fiji/VIB_ "2.0.3"]
                 ;[sc.fiji/registration_3d "2.0.0"]
                 ;[edu.mines/mines-jtk "20100113"]
                 [com.github.wendykierp/JTransforms "3.0"]                            
                 
                 ;[local/DeconvolutionLab "4.02.2014"]
                 [funimage/mcib3d_plugins "3.3"]
                 [local/mcib3d-core "3.0"]
                 [me.raynes/fs "1.4.6"]
                 
                 
                 ]
  :java-source-paths ["java"]
  :repositories [["imagej" "http://maven.imagej.net/content/groups/hosted/"]
                 ["imagej-releases" "http://maven.imagej.net/content/repositories/releases/"]
                 ;["imagej-ome" "http://maven.imagej.net/content/repositories/ome-releases/"]
                 ["ome maven" "http://artifacts.openmicroscopy.org/artifactory/maven/"]
                 ["imagej-snapshots" "http://maven.imagej.net/content/repositories/snapshots/"]
                 #_["snapshots-kyleharrington" "http://kyleharrington.com:8081/nexus/content/repositories/snapshots/"]
                 #_["thirdparty-kyleharrington" "http://kyleharrington.com:8081/nexus/content/repositories/thirdparty/"]]                 
  :jvm-opts ["-Xmx32g" "-server"] 
  :javac-options ["-target" "1.6" "-source" "1.6"]
  )
