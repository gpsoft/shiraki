(ns shiraki.gui
  (:require
   [shiraki.util :as u])
  (:import
   [java.awt Font Image]
   [java.awt.event KeyAdapter KeyEvent ComponentAdapter ComponentEvent]
   [java.awt.event ActionListener ActionEvent]
   [java.awt.event WindowAdapter WindowEvent]
   [javax.swing UIManager JOptionPane JFrame]
   [javax.swing JPanel JLabel SwingConstants ImageIcon]))

(defn look-and-feel!
  []
  (UIManager/setLookAndFeel
   (UIManager/getSystemLookAndFeelClassName)))

(defn alert!
  ([parent msg] (alert! nil msg ""))
  ([parent msg title]
   (JOptionPane/showMessageDialog
    parent msg title JOptionPane/PLAIN_MESSAGE)))

(defn create-window!
  [w h exit-on-close?]
  (doto (new JFrame)
    (.setSize w h)
    (.setLocationRelativeTo nil)
    (.setDefaultCloseOperation
     (if exit-on-close?
       JFrame/EXIT_ON_CLOSE
       JFrame/DISPOSE_ON_CLOSE))))

(defn close-window!
  [wnd]
  (->> (new WindowEvent wnd WindowEvent/WINDOW_CLOSING)
       (.dispatchEvent wnd))
  #_(.setVisible wnd false)
  #_(.dispose wnd))

(defn create-container!
  [bg-color layout]
  (doto (new JPanel)
    (.setBackground bg-color)
    (.setLayout layout)))

(defn create-image-component!
  [bg-color]
  (doto (new JLabel)
    (.setOpaque true)
    (.setBackground bg-color)
    (.setHorizontalAlignment SwingConstants/CENTER)))

(defn create-text-component!
  [bg-color fg-color font-size]
  (doto (new JLabel "" SwingConstants/CENTER)
    (.setOpaque true)
    (.setBackground bg-color)
    (.setFont (new Font "sans serif" Font/BOLD font-size))
    (.setForeground fg-color)))

(defn listen-key!
  [wnd h]
  (.addKeyListener
   wnd
   (proxy [KeyAdapter] []
     (keyPressed [e]
       (h (.getKeyCode e) e)))))

(defn listen-resize!
  [wnd h]
  (.addComponentListener
   wnd
   (proxy [ComponentAdapter] []
     (componentResized [e]
       (when (= (.getID e) ComponentEvent/COMPONENT_RESIZED)
         (h e))))))

(defn listen-closing!
  [wnd h]
  (.addWindowListener
   wnd
   (proxy [WindowAdapter] []
     (windowClosing [e]
       (h)))))

(defn listen-action!
  [compo h]
  (.addActionListener
   compo
   (reify ActionListener
     (actionPerformed [this e]
       (h e)))))

(defn img-contained
  [file w h]
  (let [fpath (.getAbsolutePath file)
        ii (new ImageIcon fpath)
        i (.getImage ii)
        io (.getImageObserver ii)
        iw (.getWidth i io)
        ih (.getHeight i io)
        ]
    (if (and (> w iw) (> h ih))
      ii
      (let [[iw ih] (u/scale-to-contain w h iw ih)]
        (new ImageIcon (.getScaledInstance i iw ih Image/SCALE_SMOOTH))))))


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
              #_(gui/alert! f "Closing!")
              #_(close! f)
              #_(System/exit 0)
              (when (full-screen? f) (full-screen! f false)))))
       _ (full-screen! f true)
       ]
   )

 )
