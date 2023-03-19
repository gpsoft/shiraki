(ns shiraki.player
  (:require
   [overtone.at-at :as atat])
  (:import
   [java.awt Image]
   [javax.swing ImageIcon]))

(defonce atat-pool (atat/mk-pool))

(defn create!
  [ttl-compo img-compo txt-compo image-files]
  (atom {:playing? false
         :interval 4000
         :ix nil  ;; what image shown now
         :ttl-compo ttl-compo
         :img-compo img-compo
         :txt-compo txt-compo
         :image-files image-files}))

(defn- move-ix
  [ix n forward?]
  (if (nil? ix) 0
    (mod ((if forward? inc dec) ix) n)))

(defn playing?
  [player]
  (:playing? @player))

(defn- size-contain
  [w h iw ih]
  (let [rw (/ w iw)
        rh (/ h ih)
        r (min rw rh)]
    [(int (* iw r)) (int (* ih r))]))

(defn- img-contain
  [file w h]
  (let [fpath (.getName file)
        ii (new ImageIcon fpath)
        i (.getImage ii)
        io (.getImageObserver ii)
        iw (.getWidth i io)
        ih (.getHeight i io)
        ]
    (if (and (> w iw) (> h ih))
      ii
      (let [[iw ih] (size-contain w h iw ih)]
        (new ImageIcon (.getScaledInstance i iw ih Image/SCALE_SMOOTH))))))

(defn- move-ix!
  [player forward?]
  (swap! player
         (fn [{:keys [ix image-files] :as st}]
           (let [new-ix (move-ix ix (count image-files) forward?)]
             #_(prn ix new-ix)
             (assoc st :ix new-ix)))))

(defn- draw!
  [{:keys [ix ttl-compo img-compo txt-compo image-files]}]
  #_(println "ix:" ix)
  (let [file (nth image-files ix)
        fname (.getName file)]
    (.setText ttl-compo (str fname "(" (inc ix) "/" (count image-files) ")"))
    (.setText txt-compo "<html>abc def xyz</html>")
    (let [ii (img-contain file
                          (.getWidth img-compo)   ;; FIX: want size of pane (not size of compo)
                          (.getHeight img-compo))]
      (.setIcon img-compo ii))))

(defn- tick!
  [player]
  (move-ix! player true)
  (draw! @player))

(defn next!
  [player]
  (tick! player))

(defn prev!
  [player]
  (move-ix! player false)
  (draw! @player))

(defn stop!
  [player]
  (swap! player
         #(assoc % :playing? false))
  (atat/stop-and-reset-pool! atat-pool))

(defn start!
  [player]
  (swap! player
         #(assoc % :playing? true))
  (let [interval (:interval @player)]
    (atat/every interval
                #(tick! player)
                atat-pool)))

(defn suspend!
  [player]
  (when (playing? player)
    (stop! player)))

(defn resume!
  [player]
  (when-not (playing? player)
    (swap! player
           #(assoc % :playing? true))
    (let [interval (:interval @player)]
      (atat/every interval
                  #(tick! player)
                  atat-pool
                  :initial-delay interval
                  ))))

(defn toggle-pause!
  [player]
  (if (playing? player)
    (suspend! player)
    (resume! player)))

(comment
 (stop! nil)
 )
