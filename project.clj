(defproject funimage "0.1.95"
  :description "Functional Image Processing with ImageJ/FIJI"
  :url "https://github.com/funimage/funimage"
  :license {:name "Apache v2.0"
            :url "https://github.com/funimage/funimage/LICENSE"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [seesaw "1.4.4"]
                 [me.raynes/fs "1.4.6"]
                 [org.clojure/data.zip "0.1.1"]

                 ; Java libs
                 [net.imglib2/imglib2-roi "0.4.5"]
                 [net.imglib2/imglib2-ij "2.0.0-beta-36"]
                 [net.imagej/imagej "2.0.0-rc-58" :exclusions [com.github.jnr/jffi com.github.jnr/jnr-x86asm]]
                 [ome/bioformats_package "5.3.3"]
                 
                 ;[sc.fiji/Auto_Threshold "1.16.0"]
                 ]
  :java-source-paths ["java"]
  :repositories [["imagej" "http://maven.imagej.net/content/groups/hosted/"]
                 ["imagej-releases" "http://maven.imagej.net/content/repositories/releases/"]
                 ["ome maven" "http://artifacts.openmicroscopy.org/artifactory/maven/"]
                 ["imagej-snapshots" "http://maven.imagej.net/content/repositories/snapshots/"]
                 ["clojars2" {:url "http://clojars.org/repo/"
                             :username :env/LEIN_USERNAME
                              :password :env/LEIN_PASSWORD}]]
  :deploy-repositories [["releases" {:url "http://maven.imagej.net/content/repositories/releases"
                                     ;; Select a GPG private key to use for
                                     ;; signing. (See "How to specify a user
                                     ;; ID" in GPG's manual.) GPG will
                                     ;; otherwise pick the first private key
                                     ;; it finds in your keyring.
                                     ;; Currently only works in :deploy-repositories
                                     ;; or as a top-level (global) setting.
                                     :username :env/CI_DEPLOY_USERNAME
                                     :password :env/CI_DEPLOY_PASSWORD
                                     :sign-releases false}]
                        ["snapshots" {:url "http://maven.imagej.net/content/repositories/snapshots"
                                      :username :env/CI_DEPLOY_USERNAME
                                      :password :env/CI_DEPLOY_PASSWORD
                                      :sign-releases false}]]
  
  :jvm-opts ["-Xmx32g" "-server"
             ;"-javaagent:/Users/kyle/.m2/repository/net/imagej/ij1-patcher/0.12.3/ij1-patcher-0.12.3.jar=init"
             #_"-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=localhost:8000"] 
  ;:javac-options ["-target" "1.6" "-source" "1.6"]
  )
