(defproject funimage "0.1.100-SNAPSHOT"
  :description "Functional Image Processing with ImageJ/FIJI"
  :url "https://github.com/funimage/funimage"
  :license {:name "Apache v2.0"
            :url "https://github.com/funimage/funimage/LICENSE"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [seesaw "1.4.4"]
                 ;[me.raynes/fs "1.4.6"]
                 ;[org.clojure/data.zip "0.1.1"]
                 [clj-random "0.1.8"]
                 
                 ;[cc.artifice/clj-ml "0.8.5"]
                 [random-forests-clj "0.2.0"]

                 ; Java libs
                 [net.imglib2/imglib2-algorithm "0.8.0"]
                 [net.imglib2/imglib2-roi "0.4.6"]
                 [net.imglib2/imglib2-ij "2.0.0-beta-37"]
                 [net.imagej/imagej "2.0.0-rc-61" :exclusions [com.github.jnr/jffi com.github.jnr/jnr-x86asm]]
                 [net.imagej/imagej-ops "0.38.1-SNAPSHOT"]
                 [net.imagej/imagej-mesh "0.1.1-SNAPSHOT"]
                 [ome/bioformats_package "5.3.3"]
                 
                 [sc.fiji/Auto_Threshold "1.16.0"]
                 ]
  :java-source-paths ["java"]
  :repositories [["imagej" "https://maven.imagej.net/content/groups/hosted/"]
                 ["imagej-releases" "https://maven.imagej.net/content/repositories/releases/"]
                 ["ome maven" "https://artifacts.openmicroscopy.org/artifactory/maven/"]
                 ["imagej-snapshots" "https://maven.imagej.net/content/repositories/snapshots/"]
                 ["clojars2" {:url "https://clojars.org/repo/"
                             :username :env/LEIN_USERNAME
                              :password :env/LEIN_PASSWORD}]]
  :deploy-repositories [["releases" {:url "https://maven.imagej.net/content/repositories/releases"
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
                        ["snapshots" {:url "https://maven.imagej.net/content/repositories/snapshots"
                                      :username :env/CI_DEPLOY_USERNAME
                                      :password :env/CI_DEPLOY_PASSWORD
                                      :sign-releases false}]]
  ; Try to use lein parent when we can
;  :plugins [[lein-parent "0.3.1"]]
  :jvm-opts ["-Xmx32g" "-server"
             ;"-javaagent:/Users/kyle/.m2/repository/net/imagej/ij1-patcher/0.12.3/ij1-patcher-0.12.3.jar=init"
             #_"-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=localhost:8000"] 
  ;:javac-options ["-target" "1.6" "-source" "1.6"]
  )
