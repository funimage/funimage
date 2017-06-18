(ns funimage.mesh
  (:import [net.imagej.mesh.stl STLFacet BinarySTLFormat])
  (:require [funimage.img :as img]
            [funimage.imp :as imp]
            [funimage.imagej :as ij]
            [funimage.imagej.ops :as ops]
            [funimage.conversion :as iconv]))

(defn vertex-to-vector3d
  "Convert a Vertex to Vector3D."
  [vtx]
  (org.apache.commons.math3.geometry.euclidean.threed.Vector3D.
    (.getX vtx)
    (.getY vtx)
    (.getZ vtx)))

(defn marching-cubes
  "Convenience function for marching cubes."
  [input]
  (funimage.imagej.ops.geom/marchingCubes (funimage.imagej.ops.convert/bit input)))

(defn write-mesh-as-stl
  "Write a DefaultMesh from imagej-ops to a .stl file."
  [mesh stl-filename]
  (let [stl-facets (for [facet (.getFacets mesh)]
                      (STLFacet. (vertex-to-vector3d (.getNormal facet)) 
                                 (vertex-to-vector3d (.getP0 facet))
                                 (vertex-to-vector3d (.getP1 facet))
                                 (vertex-to-vector3d (.getP2 facet))
                                 0))
        ofile (java.io.FileOutputStream. stl-filename)]
    (.write ofile
      (.write (BinarySTLFormat.)
        stl-facets))
    (.close ofile)))

