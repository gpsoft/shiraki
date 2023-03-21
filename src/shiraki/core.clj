(ns shiraki.core
  (:require
   [clojure.string :as string]
   [shiraki.util :as u]
   [shiraki.gui :as gui]
   [shiraki.exif :as exif]
   [shiraki.player :as player])
  (:import
   [java.awt GraphicsDevice GraphicsEnvironment Color]
   [java.awt GridBagConstraints GridBagLayout Insets]
   [java.awt.event KeyEvent]
   [javax.swing JFrame])
  (:gen-class))

(def comment-file ".shiraki.edn")

(def color-black (new Color 0.1 0.1 0.1))
(def color-dark (new Color 0.2 0.2 0.2))
(def color-light (new Color 0.8 0.8 0.8))
(def color-white (new Color 0.9 0.9 0.9))

(defn- full-screen-es?
  [compo]
  (bit-and (.getExtendedState compo) JFrame/MAXIMIZED_BOTH))
(defn- full-screen-ge?
  [compo]
  (let [ge (GraphicsEnvironment/getLocalGraphicsEnvironment)
        gd (.getDefaultScreenDevice ge)]
    (not (nil? (.getFullScreenWindow gd)))))
(defn- full-screen?
  [compo]
  (if (u/mac-os?)
    (full-screen-es? compo)
    (full-screen-ge? compo)))

(defn- full-screen-es!
  [compo toggle]
  (.setExtendedState compo (if toggle JFrame/MAXIMIZED_BOTH JFrame/NORMAL)))
(defn- full-screen-ge!
  [compo toggle]
  (let [ge (GraphicsEnvironment/getLocalGraphicsEnvironment)
        gd (.getDefaultScreenDevice ge)]
    (.setFullScreenWindow gd (if toggle compo nil))))
(defn- full-screen!
  [compo toggle]
  (let [ge (GraphicsEnvironment/getLocalGraphicsEnvironment)
        gd (.getDefaultScreenDevice ge)]
    (doto compo
      (.dispose)
      (.setUndecorated toggle)
      (.setVisible true))
    (if (u/mac-os?)
      (full-screen-es! compo toggle)
      (full-screen-ge! compo toggle))
    ;; FIX: linux: restore window size?
    (if toggle
      (doto compo    ;; mac: should be AFTER maximize
        (.toFront)
        (.requestFocus))
      (doto compo
        (.repaint)))))


(defn- set-constraints!
  ([layout compo x y w h]
   (set-constraints! layout compo x y w h {}))
  ([layout compo x y w h opts]
   (let [opts (merge {:weightx 1.0
                      :weighty 1.0
                      :anchor GridBagConstraints/CENTER
                      :fill GridBagConstraints/BOTH
                      :insets (new Insets 0 0 0 0)
                      :paddingx 0
                      :paddingy 0}
                     opts)
         {:keys [weightx weighty anchor fill
                 insets paddingx paddingy]} opts
         gbc (new GridBagConstraints
                  x y w h weightx weighty
                  anchor fill insets
                  paddingx paddingy)]
     (.setConstraints layout compo gbc))))

(defn- main-window!
  [w h exit-on-close?]
  (let [fr (gui/create-window! w h exit-on-close?)]
    (gui/listen-resize! fr (fn [e] #_(println "resized")))
    fr))

(defn- main-container!
  [bg-color layout]
  (gui/create-container! bg-color layout))

(defn- image-compo!
  [bg-color]
  (gui/create-image-component! bg-color))

(defn- text-compo!
  [bg-color font-size]
  (gui/create-text-component! bg-color color-white font-size))

(defn- image-timestamp
  [file]
  (let [exif-timestamp (:datetime-digitized (exif/extract file))]
    (if (nil? exif-timestamp)
      (u/file-timestamp-in-exif-format file)
      exif-timestamp)))

(defn- all-images
  [path-str]
  (u/find-image-files path-str #(image-timestamp %)))

(defn- comment-map
  [path-str]
  (u/read-edn path-str comment-file))

(defn- render-exif
  [file]
  (let [exif (exif/extract file)
        items (exif/render exif)]
    (->> items
         (map (fn [[nm v]] (str nm ": " v)))
         (string/join "\n"))))

(defn- go!
  ([] (go! "." 1000 false))
  ([path-str] (go! path-str 1000 false))
  ([path-str interval from-main?]
   (gui/look-and-feel!)
   (let [wnd (main-window! 600 400 from-main?)
         layout (new GridBagLayout)
         container (main-container! color-black layout)
         title-compo (text-compo! color-black 18)
         image-compo (image-compo! color-dark)
         comment-compo (text-compo! color-black 36)
         images (all-images path-str)
         comments (comment-map path-str)
         player (player/create!
                 title-compo image-compo comment-compo
                 images interval comments)
         ]
     (set-constraints! layout title-compo 0 0 1 1
                       {:weighty 0 :paddingy 8})
     (set-constraints! layout image-compo 0 1 1 5)
     (set-constraints! layout comment-compo 0 6 1 2
                       {:weighty 0 :paddingy 16})
     (.add container title-compo)
     (.add container image-compo)
     (.add container comment-compo)
     (.add (.getContentPane wnd) container)
     (full-screen! wnd true)
     #_(.setVisible wnd true)
     (future   ;; wait for layout to settle down
      (player/start! player))
     (gui/listen-closing! wnd #(player/stop! player))
     (gui/listen-key! wnd
                  (fn [c e]
                    #_(prn c)
                    (when (= c KeyEvent/VK_RIGHT)
                      (player/next! player))
                    (when (= c KeyEvent/VK_LEFT)
                      (player/prev! player))
                    (when (= c KeyEvent/VK_SPACE)
                      (player/toggle-pause! player))
                    (when (= c KeyEvent/VK_I)
                      (when-let [file (nth images (player/index player) nil)]
                        (gui/alert! wnd (render-exif file))))
                    (when (or (= c KeyEvent/VK_ESCAPE)
                              (= c KeyEvent/VK_Q))
                      #_(full-screen! wnd false)
                      (gui/close-window! wnd)) ))

     nil)))

(defn -main
  [& args]
  (let [path-str (nth args 0 ".")
        interval (Integer/parseInt (nth args 1 "4000"))]
    (go! path-str interval true)))

