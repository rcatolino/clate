(ns drplate.core
  (:gen-class
    :name drplate.core
    :methods [#^{:static true} [templateFile [java.lang.String java.lang.String boolean] void]
              #^{:static true} [insertVar [java.lang.String java.lang.String] void]
              #^{:static true} [insertVar [java.lang.String int] void]
              #^{:static true} [insertVar [java.lang.String boolean] void]
              #^{:static true} [init [] void]
              #^{:static true} [getVar [java.lang.String] java.lang.String]]))

(declare process-regular process-macro get-var)

(defn -insertVar [^java.lang.String varname value]
  (eval (read-string (str "(def ^:dymamic " varname " " (pr-str value) ")"))))

(defn -init []
  (eval '(def ^:dymamic version "0.1.0")))

(defn flush-text [buff result-stream]
  (.write result-stream (clojure.string/join buff))
  [])

(defn flush-macro [buff result-stream]
  (let [macro (clojure.string/join buff)]
    (try
      (let [expanded (eval (read-string macro))]
        (if (string? expanded)
          (.write result-stream expanded)
          (println "New inner definition :\n" macro)))
    (catch RuntimeException cex
        (println (str "Error while expanding macro \""
                      macro
                      "\" : "
                      (.getMessage cex))))))
  [])

(defn split-on-macro-start [line]
  (clojure.string/split line #"\{%" 2))

(defn split-on-macro-end [line]
  (clojure.string/split line #"%\}" 2))

(defn process-macro [lines macro-buff regular-buff result-stream nesting-lvl]
  (if (empty? lines)
    (println "Error unexpected end-of file wile reading macro")
    (let [splited-end (split-on-macro-end (first lines))
          splited-start (split-on-macro-start (first lines))]
      (if (< (count (first splited-start)) (count (first splited-end)))
        #(process-macro (cons (second splited-start) (rest lines))
                        (conj macro-buff (str (first splited-start) "{%"))
                        regular-buff
                        result-stream
                        (inc nesting-lvl))
        (if (second splited-end)
          (if (> nesting-lvl 0)
            #(process-macro (cons (second splited-end) (rest lines))
                            (conj macro-buff (str (first splited-end) "%}"))
                            regular-buff
                            result-stream
                            (dec nesting-lvl))
            #(process-regular (cons (second splited-end) (rest lines))
                              (flush-macro (conj macro-buff (first splited-end)) result-stream)
                              regular-buff
                              result-stream))
          #(process-macro (rest lines)
                          (conj macro-buff (str (first splited-end) "\n"))
                          regular-buff
                          result-stream
                          nesting-lvl))))))

(defn process-regular [lines macro-buff regular-buff result-stream]
  (if (empty? lines)
    (flush-text regular-buff result-stream)
    (let [splited (split-on-macro-start (first lines))]
      (if (second splited)
        #(process-macro (cons (second splited) (rest lines))
                        macro-buff
                        (flush-text (conj regular-buff (first splited)) result-stream)
                        result-stream
                        0)
        #(process-regular (rest lines)
                          macro-buff
                          (conj regular-buff (str (first splited) "\n"))
                          result-stream)))))

(defn start-processing [lines result-stream double-pass]
      (if double-pass
        (do (with-open [temp-stream (clojure.java.io/writer "/tmp/plate.drl")]
              (trampoline process-regular lines [] [] temp-stream))
            (with-open [temp-stream (clojure.java.io/reader "/tmp/plate.drl")]
              (trampoline process-regular (line-seq temp-stream) [] [] result-stream)))
        (trampoline process-regular lines [] [] result-stream)))

(defn -templateFile
  "Open file and write to it"
  [template-name result-name & args]
  (alter-var-root #'*read-eval* (constantly false))
  ;(eval '(defn get-var [key]
  ;  (get vars key)))
  ;(eval ('def '^:dynamic 'vars (assoc vars :test "value")))
  (with-open [template-stream (clojure.java.io/reader template-name)]
    (if (= result-name "*out*")
      (start-processing (line-seq template-stream)
                        *out*
                        (and (= (count args) 1) (first args)))
      (with-open [result-stream (clojure.java.io/writer result-name)]
        (start-processing (line-seq template-stream)
                          result-stream
                          (and (= (count args) 1) (first args)))))))

(defn -main
  [template-name result-name & args]
  (alter-var-root #'*read-eval* (constantly false))
  (if (> (count args) 1)
    (println "Too many arguments")
    (-templateFile template-name result-name args)))
