(ns funimage.filters.gabor
  (:use [funimage imp project utils]
        [funimage.imp calculator roi]
        [funimage.segmentation utils]))

(defn make-odd [n] (if (not (odd? n))
                     (+ n 1)
                     n))
(defn gauss-0
  [x y sigma]
  (java.lang.Math/exp (/ (- 0 (+ (* x x) (* y y))) (* 2 sigma sigma))))

(defn gauss 
  [x y x-sigma y-sigma]
  (java.lang.Math/exp (* -1 (+ (/ (* x x) x-sigma) (/ (* y y) y-sigma)))))
 
(defn gabor-0
  "takes wavelength, direction, returns kernel"
  [wavelength direction]
  (let [octave 1
        sigma (* wavelength (/ 1 Math/PI) (Math/sqrt (/ (Math/log 2) 2)) (/ (+ (Math/pow 2 octave) 1) (+ (Math/pow 2 octave) -1)))
        x (java.lang.Math/ceil (Math/sqrt (* -2 sigma sigma (Math/log 0.005))))
        row (+ (* 2 x) 1)
        columns row
        row-center (/ (- row 1) 2)
        col-center (/ (- columns 1) 2)
        y-range (map #(+ (- 0 row-center) %) (flatten (map #(repeat columns %) (range row))))
        x-range (map #(+ (- 0 col-center) %) (flatten (repeat row (range columns))))
        gaussian (map #(gauss-0 %1 %2 sigma) x-range y-range)
        theta (map #(+ %1 %2) (map #(* (java.lang.Math/cos direction) %) x-range) (map #(* (java.lang.Math/sin direction) %) y-range))
        fourier (map #(java.lang.Math/cos %) (map #(* 2 Math/PI (/ % wavelength))theta))
        gabor (map #(* %1 %2) gaussian fourier)]
    [(float-array gabor) (int row)]))

(defn gabor-fixed
  "takes wavelength, direction, aspect; returns kernel"
  [wavelength direction gamma]
  (let [psi 0
        x-sigma (* wavelength 2 Math/PI)
        y-sigma (/ x-sigma gamma)
        nstds 0.5
        row (max (java.lang.Math/ceil 
                   (max 1 
                        (max (java.lang.Math/abs (* nstds x-sigma (java.lang.Math/cos direction)))
                             (java.lang.Math/abs (* nstds y-sigma (java.lang.Math/sin direction))))))
       
                 (java.lang.Math/ceil 
                   (max 1 
                        (max (java.lang.Math/abs (* nstds x-sigma (java.lang.Math/sin direction))) 
                             (java.lang.Math/abs (* nstds y-sigma (java.lang.Math/cos direction)))))))
        columns row
        row-center (/ (+ row 1) 2)
        col-center (/ (+ columns 1) 2)
        x-range (map #(+ (- 0 col-center) %) (flatten (repeat row (range columns))))
        y-range (map #(+ (- 0 row-center) %) (flatten (map #(repeat columns %) (range row))))
        x-theta (map #(+ %1 %2) (map #(* (java.lang.Math/cos direction) %) x-range) (map #(* (java.lang.Math/sin direction) %) y-range))
        y-theta (map #(+ %1 %2) (map #(* (java.lang.Math/sin direction) %) (map unchecked-negate x-range)) (map #(* (java.lang.Math/cos direction) %) y-range))
        gaussian (map #(gauss %1 %2 x-sigma y-sigma) x-theta y-theta)
        
        true-size (make-odd (int (java.lang.Math/ceil (java.lang.Math/sqrt (count (remove #(< % 0.001) gaussian))))))
        row true-size
        columns true-size
        row-center (/ (- row 1) 2)
        col-center (/ (- columns 1) 2)
        x-range (map #(+ (- 0 col-center) %) (flatten (repeat row (range columns))))
        y-range (map #(+ (- 0 row-center) %) (flatten (map #(repeat columns %) (range row))))
        x-theta (map #(+ %1 %2) (map #(* (java.lang.Math/cos direction) %) x-range) (map #(* (java.lang.Math/sin direction) %) y-range))
        y-theta (map #(+ %1 %2) (map #(* (java.lang.Math/sin direction) %) (map unchecked-negate x-range)) (map #(* (java.lang.Math/cos direction) %) y-range))
        gaussian (map #(gauss %1 %2 x-sigma y-sigma) x-theta y-theta)
        
        fourier (map #(java.lang.Math/cos %) (map #(* 2 Math/PI (/ % wavelength)) (map #(+ psi %) x-theta)))
        gabor (map #(* %1 %2) gaussian fourier)]
    [(float-array gabor) (int row)]))

(defn gabor
  "takes wavelength, direction, returns kernel"
  [wavelength direction sigma psi gamma dim]
  (let [x-sigma sigma
        y-sigma (/ x-sigma gamma)
        row dim
        columns row
        row-center (/ (+ row 1) 2)
        col-center (/ (+ columns 1) 2)
        x-range (map #(+ (- 0 col-center) %) (flatten (repeat row (range columns))))
        y-range (map #(+ (- 0 row-center) %) (flatten (map #(repeat columns %) (range row))))
        x-theta (map #(+ %1 %2) (map #(* (java.lang.Math/cos direction) %) x-range) (map #(* (java.lang.Math/sin direction) %) y-range))
        y-theta (map #(+ %1 %2) (map #(* (java.lang.Math/sin direction) %) (map unchecked-negate x-range)) (map #(* (java.lang.Math/cos direction) %) y-range))
        gaussian (map #(gauss %1 %2 x-sigma y-sigma) x-theta y-theta)
        fourier (map #(java.lang.Math/cos %) (map #(* 2 Math/PI (/ % wavelength)) (map #(+ psi %) x-theta)))
        gabor (map #(* %1 %2) gaussian fourier)]
    [(float-array gabor) (int row)]))

