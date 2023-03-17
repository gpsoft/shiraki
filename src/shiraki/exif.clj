(ns shiraki.exif
  (:import
   [com.drew.metadata Metadata]
   [com.drew.imaging ImageMetadataReader]))

(def dir-tags
  {"JPEG" {"Image Height" :height
           "Image Width" :width}
   "Exif IFD0" {"Model" :model
                "Date/Time" :datetime-last-modified}
   "Exif SubIFD" {"Exposure Time" :ss
                  "F-Number" :f
                  "ISO Speed Ratings" :iso
                  "Date/Time Original" :datetime-original
                  "Date/Time Digitized" :datetime-digitized
                  "Exposure Bias Value" :exp-compensation
                  "Focal Length" :focal-length
                  "Focal Length 35" :focal-length-in-35}
   "File" {"File Name" :file-name
           "File Size" :file-size}})

(def items
  [[:file-name "ファイル名"]
   [:width "幅"]
   [:height "高さ"]
   [:file-size "ファイルサイズ"]
   [:model "カメラ"]
   [:focal-length "焦点距離"]
   [:focal-length-in-35 "焦点距離(35mm換算)"]
   [:ss "シャッター速度"]
   [:f "絞り"]
   [:iso "ISO"]
   [:exp-compensation "露出補正"]
   [:datetime-original "撮影日時"]
   [:datetime-digitized "デジタル化日時"]
   [:datetime-last-modified "更新日時"]])

(defn- all-tags
  [m]
  (mapcat #(.getTags %) (seq (.getDirectories m))))

(defn extract
  [f]
  (let [
        m (ImageMetadataReader/readMetadata f)
        tags (all-tags m)]
    (reduce (fn [acc t]
              (let [dir-name (.getDirectoryName t)
                    tag-name (.getTagName t)
                    kw (get-in dir-tags [dir-name tag-name])]
                (if kw
                  (assoc acc kw (.getDescription t))
                  acc)))
            {} tags)))

(defn render
  [exif]
  (filter some?
          (map
           (fn [[kw nm]]
             (when-let [v (get exif kw)] [nm v]))
           items)))

(comment
 (extract (clojure.java.io/file "./1.jpg"))
 (render (extract (clojure.java.io/file "./1.jpg")))
 )

