
;; This is the old Clojure REPL
;(.run (Clojure.Clojure_Interpreter.) "")

(import '[org.scijava Context]
        '[org.scijava.ui.swing.script InterpreterWindow]
        '[org.scijava.script ScriptService])
;        '[org.scijava.`object`.ObjectService

(def context (Context.))
(def interpreter-window (InterpreterWindow. context))  

(.show interpreter-window)
(.lang (.getREPL interpreter-window) "Clojure")
;(.eval (.getInterpreter interpreter-window) "(println \"Hello from Clojure\")")
;(.print "Hello!")



