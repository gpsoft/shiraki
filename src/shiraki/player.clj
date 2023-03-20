(ns shiraki.player
  (:require
   [overtone.at-at :as atat])
  (:import
   [java.awt Image]
   [javax.swing ImageIcon]))


;;; Timer
(defonce atat-pool (atat/mk-pool))
(defn- timer-running?
  []
  (if (atat/scheduled-jobs atat-pool) true false))
(defn- timer-stop!
  []
  (atat/stop-and-reset-pool! atat-pool))
(defn- timer-start!
  [handler interval]
  (let [running? (timer-running?)]
    (when running? (timer-stop!))
    (atat/every interval
                handler
                atat-pool
                :initial-delay (if running? interval 0))))


;;; Utils
(defn- move-ix
  [ix n forward?]
  (if (or (nil? ix) (zero? n)) 0
    (mod ((if forward? inc dec) ix) n)))
(defn- size-contain
  [w h iw ih]
  (let [rw (/ w iw)
        rh (/ h ih)
        r (min rw rh)]
    [(int (* iw r)) (int (* ih r))]))


;;; State and primitive operations
(defn- st-create!
  [ttl-compo img-compo txt-compo image-files]
  (atom {:playing? false
         :interval 4000
         :ix nil  ;; what image shown now
         :ttl-compo ttl-compo
         :img-compo img-compo
         :txt-compo txt-compo
         :image-files image-files}))
(defn- st-playing?
  [st]
  (:playing? st))
(defn- st-index
  [st]
  (:ix st))
(defn- st-interval
  [st]
  (:interval st))
(defn- st-move-ix!
  [player forward?]
  (swap! player
         (fn [{:keys [ix image-files] :as st}]
           (let [new-ix (move-ix ix (count image-files) forward?)]
             #_(prn ix new-ix)
             (assoc st :ix new-ix)))))
(defn- st-play!
  [player starting?]
  (when-not (= (st-playing? @player) starting?)
    (swap! player
           #(assoc % :playing? starting?))))


(defn- img-contain
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
      (let [[iw ih] (size-contain w h iw ih)]
        (new ImageIcon (.getScaledInstance i iw ih Image/SCALE_SMOOTH))))))

(defn- draw!
  [{:keys [ix ttl-compo img-compo txt-compo image-files]}]
  #_(println "ix:" ix)
  (try
   (let [file (nth image-files ix)
         fname (.getName file)]
     (.setText ttl-compo (str fname "(" (inc ix) "/" (count image-files) ")"))
     (.setText txt-compo "<html>abc def xyz</html>")
     (let [ii (img-contain file
                           (.getWidth img-compo)   ;; FIX: want size of pane (not size of compo)
                           (.getHeight img-compo))]
       (.setIcon img-compo ii)))
   (catch Exception ex
     (.setText ttl-compo "No image found"))))

(defn- tick!
  [player]
  (st-move-ix! player true)
  (draw! @player))




;;; API
(defn create!
  [ttl-compo img-compo txt-compo image-files]
  (st-create! ttl-compo img-compo txt-compo image-files))

(defn playing?
  [player]
  (st-playing? @player))

(defn index
  [player]
  (st-index @player))

(defn next!
  [player]
  (tick! player)
  (when (st-playing? @player)
    (timer-start! #(tick! player) (st-interval @player))))

(defn prev!
  [player]
  (st-move-ix! player false)
  (draw! @player)
  (when (st-playing? @player)
    (timer-start! #(tick! player) (st-interval @player))))

(defn stop!
  [player]
  (timer-stop!)
  (st-play! player false))

(defn start!
  [player]
  (st-play! player true)
  (timer-start! #(tick! player) (st-interval @player)))

(defn suspend!
  [player]
  (when (st-playing? @player)
    (stop! player)))

(defn resume!
  [player]
  (st-play! player true)
  (timer-start! #(tick! player) (st-interval @player)))

(defn toggle-pause!
  [player]
  (if (st-playing? @player)
    (suspend! player)
    (resume! player)))

(comment
 (stop! nil)
 )
