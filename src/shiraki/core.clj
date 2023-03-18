(ns shiraki.core
  (:require
   [shiraki.exif :as exif]
   [shiraki.player :as player])
  (:import
   [java.awt GraphicsDevice GraphicsEnvironment Font Color]
   [java.awt GridBagConstraints GridBagLayout Insets]
   [java.awt.event KeyListener KeyEvent ComponentListener ComponentEvent]
   [java.awt.event ActionListener ActionEvent]
   [java.awt.event WindowAdapter WindowEvent]
   [javax.swing UIManager JOptionPane JFrame JLabel]
   [javax.swing JPanel SwingConstants])
  (:gen-class))

(defn- look-and-feel!
  []
  (UIManager/setLookAndFeel
   (UIManager/getSystemLookAndFeelClassName)))

(defn- alert!
  [m]
  (JOptionPane/showMessageDialog nil m))

(defn- full-screen?
  [compo]
  (let [ge (GraphicsEnvironment/getLocalGraphicsEnvironment)
        gd (.getDefaultScreenDevice ge)]
    (not (nil? (.getFullScreenWindow gd)))))

(defn- full-screen!
  [compo toggle]
  (let [ge (GraphicsEnvironment/getLocalGraphicsEnvironment)
        gd (.getDefaultScreenDevice ge)]
    (if toggle
      (do
       (doto compo
         (.dispose)
         (.setUndecorated true)
         (.setVisible true))
       (.setFullScreenWindow gd compo)
       (doto compo    ;; mac: should be AFTER maximize
         (.toFront)
         (.requestFocus)))
      (do
       (.setFullScreenWindow gd nil)
       (doto compo
         (.dispose)
         (.setUndecorated false)
         ;; FIX: linux: restore window size?
         (.setVisible true)
         (.repaint))))))

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
                      :fill GridBagConstraints/NONE
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
    (listen-key! fr (fn [c e]
                      (when (= c KeyEvent/VK_ESCAPE)
                        #_(full-screen! fr false)
                        ;; close main window
                        (close! fr)
                        (when exit-on-close?
                          (System/exit 0))) ))
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
  [bg-color]
  (doto (new JLabel "" SwingConstants/CENTER)
    (.setFont (new Font "sans serif" Font/BOLD 36))
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

(defn- all-images
  [path-str]
  (let [d (clojure.java.io/file path-str)
        files (filter #(image-file? %) (.listFiles d))]
    (into [] (sort-by #(:datetime-digitized (exif/extract %)) files))))

(defn- go!
  ([] (go! false))
  ([from-main?]
   (let [wnd (main-window! 600 200 from-main?)
         bg-color (new Color 0.2 0.2 0.2)
         layout (new GridBagLayout)
         container (main-container! bg-color layout)
         image-compo (image-compo! (new Color 0.8 0.8 0.8))
         text-compo (text-compo! bg-color)
         images (all-images ".")
         player (player/create! image-compo text-compo images)
         ]
     (set-constraints! layout image-compo 0 0 1 3
                       {:fill GridBagConstraints/BOTH :paddingx 0 :paddingy 0})
     (set-constraints! layout text-compo 0 3 1 1
                       {:fill GridBagConstraints/BOTH :weighty 0.5})
     (.add container image-compo)
     (.add container text-compo)
     (.add (.getContentPane wnd) container)
     (full-screen! wnd true)
     #_(.setVisible wnd true)
     (player/start! player)
     (listen-closing! wnd #(player/stop! player))
     nil)))

(defn -main
  [& args]
  (go! true))

(comment
 (look-and-feel!)
 (alert! "hey")

 (let [f (doto (new JFrame)
           (.setSize 300 200)
           (.setLocationRelativeTo nil))
       _ (listen-key!
          f
          (fn [c e]
            (when (= c KeyEvent/VK_ESCAPE)
              #_(alert! "Closing!")
              #_(close! f)
              #_(System/exit 0)
              (when (full-screen? f) (full-screen! f false)))))
       _ (full-screen! f true)
       ]
   )

 )
