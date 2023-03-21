(ns shiraki.util
  (:require
   [clojure.java.io :as io]
   [clojure.edn :as edn])
  (:import
   [java.time Instant ZoneId]
   [java.time.format DateTimeFormatter]))

(defn mac-os?
  []
  (let [os-name (System/getProperty "os.name")]
    (.startsWith os-name "Mac")))

(defn image-file?
  [file]
  (and (.isFile file)
       (re-matches #"(?i).*jpe?g$" (.getName file))))

(defn file-timestamp-in-exif-format
  [file]
  (let [unixtime (quot (.lastModified file) 1000)
        fmt (DateTimeFormatter/ofPattern "yyyy:MM:dd HH:mm:ss")]  ;; exif-like format
    (-> (Instant/ofEpochSecond unixtime)
        (.atZone (ZoneId/systemDefault))
        (.format fmt))))

(defn find-image-files
  [dir-path-str sort-fn]
  (->> (io/file dir-path-str)
       (.listFiles)
       (filter #(image-file? %))
       (sort-by sort-fn)
       (into [])))

(defn read-edn
  [dir-path-str fname-str]
  (let [d (io/file dir-path-str)
        f-str (-> d
                  (.toPath)
                  (.resolve fname-str)
                  (.toString))]
    (if (.exists (io/file f-str))
      (-> f-str
          (slurp)
          (edn/read-string))
      {})))

(defn move-ix
  [ix n forward?]
  (if (zero? n)
    0
    (mod ((if forward? inc dec) ix) n)))

(defn scale-to-contain
  [tow toh fromw fromh]
  (let [rw (/ tow fromw)
        rh (/ toh fromh)
        r (min rw rh)]
    [(int (* fromw r)) (int (* fromh r))]))
