(ns shiraki.core
  (:require
   [clojure.string :as string]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [shiraki.exif :as exif]
   [shiraki.player :as player])
  (:import
   [java.time Instant ZoneId]
   [java.time.format DateTimeFormatter]
   [java.awt GraphicsDevice GraphicsEnvironment Font Color]
   [java.awt GridBagConstraints GridBagLayout Insets]
   [java.awt.event KeyListener KeyEvent ComponentListener ComponentEvent]
   [java.awt.event ActionListener ActionEvent]
   [java.awt.event WindowAdapter WindowEvent]
   [javax.swing UIManager JOptionPane JFrame JLabel]
   [javax.swing JPanel SwingConstants])
  (:gen-class))

(def comment-file ".shiraki.edn")

(defn- look-and-feel!
  []
  (UIManager/setLookAndFeel
   (UIManager/getSystemLookAndFeelClassName)))

(defn- alert!
  ([parent msg] (alert! nil msg ""))
  ([parent msg title]
   (JOptionPane/showMessageDialog parent msg title JOptionPane/PLAIN_MESSAGE)))

(defn- mac-os?
  []
  (let [os-name (System/getProperty "os.name")]
    (.startsWith os-name "Mac")))

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
  (if (mac-os?)
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
    (if (mac-os?)
      (full-screen-es! compo toggle)
      (full-screen-ge! compo toggle))
    ;; FIX: linux: restore window size?
    (if toggle
      (doto compo    ;; mac: should be AFTER maximize
        (.toFront)
        (.requestFocus))
      (doto compo
        (.repaint)))))

(defn- close!
  [compo]
  (.dispatchEvent compo
                  (new WindowEvent compo WindowEvent/WINDOW_CLOSING))
  #_(.setVisible compo false)
  #_(.dispose compo))

(defn- listen-key!
  [compo h]
  (.addKeyListener
   compo
   (reify KeyListener
     (keyPressed [this e] (h (.getKeyCode e) e))
     (keyReleased [this e])
     (keyTyped [this e]))))

(defn- listen-resize!
  [compo h]
  (.addComponentListener
   compo
   (reify ComponentListener
     (componentResized [this e]
       (when (= (.getID e) ComponentEvent/COMPONENT_RESIZED)
         (h e)))
     (componentHidden [this e])
     (componentMoved [this e])
     (componentShown [this e]))))

(defn- listen-closing!
  [compo h]
  (.addWindowListener
   compo
   (proxy [WindowAdapter] []
     (windowClosing [e]
       (h)))))

(defn- listen-action!
  [compo h]
  (.addActionListener
   compo
   (reify ActionListener
     (actionPerformed [this e]
       (h e)))))

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
         {:keys [weightx weighty anchor fill insets paddingx paddingy]} opts
         gbc (new GridBagConstraints
                  x y w h weightx weighty
                  anchor fill insets
                  paddingx paddingy)]
     (.setConstraints layout compo gbc))))

(defn- main-window!
  [w h exit-on-close?]
  (let [fr (doto (new JFrame)
             (.setSize w h)
             (.setLocationRelativeTo nil)
             (.setDefaultCloseOperation
              (if exit-on-close?
                JFrame/EXIT_ON_CLOSE
                JFrame/DISPOSE_ON_CLOSE)))]
    (listen-resize! fr (fn [e] #_(println "resized")))
    fr))

(defn- main-container!
  [bg-color layout]
  (doto (new JPanel)
    (.setBackground bg-color)
    (.setLayout layout)))

(defn- image-compo!
  [bg-color]
  (doto (new JLabel)
    (.setOpaque true)
    (.setBackground bg-color)
    (.setHorizontalAlignment SwingConstants/CENTER)))

(defn- text-compo!
  [bg-color font-size]
  (doto (new JLabel "" SwingConstants/CENTER)
    (.setFont (new Font "sans serif" Font/BOLD font-size))
    (.setOpaque true)
    (.setForeground Color/WHITE)
    (.setBackground bg-color)))

(defn- set-text!
  [compo t]
  (.setText compo (str "<html>" t "</html>")))

(defn- image-file?
  [file]
  (and (.isFile file)
       (re-matches #"(?i).*jpg$" (.getName file))))

(defn- file-timestamp
  [file]
  (let [unixtime (quot (.lastModified file) 1000)
        fmt (DateTimeFormatter/ofPattern "yyyy:MM:dd HH:mm:ss")]  ;; exif-like format
    (-> (Instant/ofEpochSecond unixtime)
        (.atZone (ZoneId/systemDefault))
        (.format fmt))))

(defn- image-timestamp
  [file]
  (let [exif-timestamp (:datetime-digitized (exif/extract file))]
    (if (nil? exif-timestamp)
      (file-timestamp file)
      exif-timestamp)))

(defn- all-images
  [path-str]
  (let [d (io/file path-str)
        files (filter #(image-file? %) (.listFiles d))]
    (->> files
         (sort-by #(image-timestamp %))
         (into []))))

(defn- comment-map
  [path-str]
  (let [d (io/file path-str)
        f-str (-> d
                  (.toPath)
                  (.resolve comment-file)
                  (.toString))]
    (when (.exists (io/file f-str))
      (-> f-str
          (slurp)
          (edn/read-string)))))

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
   (let [wnd (main-window! 600 400 from-main?)
         bg-color (new Color 0.1 0.1 0.1)
         layout (new GridBagLayout)
         container (main-container! bg-color layout)
         title-compo (text-compo! bg-color 18)
         image-compo (image-compo! (new Color 0.2 0.2 0.2))
         text-compo (text-compo! bg-color 36)
         images (all-images path-str)
         comments (comment-map path-str)
         player (player/create!
                 title-compo image-compo text-compo
                 images interval comments)
         ]
     (set-constraints! layout title-compo 0 0 1 1
                       {:weighty 0 :paddingy 8})
     (set-constraints! layout image-compo 0 1 1 5)
     (set-constraints! layout text-compo 0 6 1 2
                       {:weighty 0 :paddingy 16})
     (.add container title-compo)
     (.add container image-compo)
     (.add container text-compo)
     (.add (.getContentPane wnd) container)
     (full-screen! wnd true)
     #_(.setVisible wnd true)
     (future   ;; wait for layout to settle down
      (player/start! player))
     (listen-closing! wnd #(player/stop! player))
     (listen-key! wnd
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
                        (alert! wnd (render-exif file))))
                    (when (or (= c KeyEvent/VK_ESCAPE)
                              (= c KeyEvent/VK_Q))
                      #_(full-screen! wnd false)
                      (close! wnd)) ))

     nil)))

(defn -main
  [& args]
  (let [path-str (nth args 0 ".")
        interval (Integer/parseInt (nth args 1 "4000"))]
    (go! path-str interval true)))

(comment
 (look-and-feel!)
 (alert! nil "hey")

 (let [f (doto (new JFrame)
           (.setSize 300 200)
           (.setLocationRelativeTo nil))
       _ (listen-key!
          f
          (fn [c e]
            (when (= c KeyEvent/VK_ESCAPE)
              #_(alert! f "Closing!")
              #_(close! f)
              #_(System/exit 0)
              (when (full-screen? f) (full-screen! f false)))))
       _ (full-screen! f true)
       ]
   )

 )
