(ns shiraki.core
  (:import
   [java.awt GraphicsDevice GraphicsEnvironment Image Font Color]
   [java.awt GridBagConstraints GridBagLayout Insets]
   [java.awt.event KeyListener KeyEvent ComponentListener ComponentEvent]
   [javax.swing UIManager JOptionPane JFrame JLabel ImageIcon]
   [javax.swing JPanel SwingConstants]
   )
  (:gen-class))

(defn- look-and-feel!
  []
  (UIManager/setLookAndFeel
   (UIManager/getSystemLookAndFeelClassName)))

(defn- alert!
  [m]
  (JOptionPane/showMessageDialog nil m))

(defn- full-screen!
  [f toggle]
  (let [ge (GraphicsEnvironment/getLocalGraphicsEnvironment)
        gd (.getDefaultScreenDevice ge)]
    (if toggle
      (do
       (doto f
         (.dispose)
         (.setUndecorated true)
         (.setVisible true)
         (.toFront)
         (.requestFocus))
       (.setFullScreenWindow gd f))
      (do
       (.setFullScreenWindow gd nil)
       (doto f
         (.dispose)
         (.setUndecorated false)
         ;; FIX: restore window size?
         (.setVisible true)
         (.repaint))))))

(defn- close!
  [f]
  (.setVisible f false)
  (.dispose f))

(defn- listen-key!
  [f h]
  (.addKeyListener
   f
   (reify KeyListener
     (keyPressed [this e] (h (.getKeyCode e) e))
     (keyReleased [this e])
     (keyTyped [this e]))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

(comment
 (look-and-feel!)
 (alert! "hey")
 (let [
       f (new JFrame)
       ; _ (.setDefaultCloseOperation f JFrame/EXIT_ON_CLOSE)
       _ (.setSize f 300 200)
       _ (.setLocationRelativeTo f nil)
       _ (.addKeyListener
          f
          (reify KeyListener
            (keyPressed [this e]
              (when (= (.getKeyCode e) KeyEvent/VK_ESCAPE)
                (full-screen! f false)))
            (keyReleased [this e])
            (keyTyped [this e])))
       _  (.addComponentListener
           f
           (reify ComponentListener
             (componentResized [this e]
               (when (= (.getID e) ComponentEvent/COMPONENT_RESIZED)
                 (println "Resized")))
             (componentHidden [this e])
             (componentMoved [this e])
             (componentShown [this e])))
       p (new JPanel)
       gbl (new GridBagLayout)
       _ (.setLayout p gbl)
       gbc (new GridBagConstraints
                0 0
                1 3
                1.0 1.0
                GridBagConstraints/CENTER
                GridBagConstraints/BOTH
                (new Insets 0 0 0 0)
                0 0)
       ; _ (set! (. gbc fill) GridBagConstraints/BOTH)
       l (new JLabel)
       _ (.setOpaque l true)
       _ (.setBackground l (new Color 0.2 0.2 0.2))
       _ (.setConstraints gbl l gbc)
       ii (new ImageIcon "./1.jpg")
       i (.getImage ii)
       ii (new ImageIcon (.getScaledInstance i 300 300 Image/SCALE_SMOOTH))
       _ (.setIcon l ii)
       _ (.setHorizontalAlignment l SwingConstants/CENTER)
       ; _ (.setText l "This is a ...")
       ; _ (.setHorizontalTextPosition l SwingConstants/CENTER)
       _ (.add p l)
       gbc (new GridBagConstraints
                0 3
                1 1
                1.0 0.5
                GridBagConstraints/CENTER
                GridBagConstraints/BOTH
                (new Insets 0 0 0 0)
                0 0)
       l (new JLabel "<html>あいうえお かきくけこ</html>" SwingConstants/CENTER)
       _ (.setFont l (new Font "sans serif" Font/BOLD 36))
       _ (.setOpaque l true)
       _ (.setForeground l Color/WHITE)
       _ (.setBackground l (new Color 0.2 0.2 0.2))
       _ (.setConstraints gbl l gbc)
       _ (.add p l)
       _ (.add (.getContentPane f) p)
       _ #_(full-screen! f true) (.setVisible f true)
       ]
   )
 (let [f (doto (new JFrame)
           (.setSize 300 200)
           (.setLocationRelativeTo nil))
       _ (listen-key! f (fn [c e]
                          (when (= c KeyEvent/VK_ESCAPE)
                            #_(alert! "Closing!")
                            #_(close! f)
                            #_(System/exit 0)
                            (full-screen! f false))))
       _ (full-screen! f true)
       ]
   )
 ; (.addActionListener my-button
 ;                         (reify ActionListener
 ;                           (actionPerformed [this e] (println "Clicked"))))
 )
