(ns shiraki.player
  (:require
   [overtone.at-at :as atat])
  (:import
   [java.awt Image]
   [javax.swing ImageIcon]))

(defonce atat-pool (atat/mk-pool))

(defn create!
  [ttl-compo img-compo txt-compo image-files]
  (atom {:ix 0
         :ttl-compo ttl-compo
         :img-compo img-compo
         :txt-compo txt-compo
         :image-files image-files}))

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

(defn- draw!
  [{:keys [ix ttl-compo img-compo txt-compo image-files]}]
  (println "ix:" ix)
  (let [file (nth image-files ix)
        fname (.getName file)
        ]
    (.setText ttl-compo fname)
    (.setText txt-compo "<html>abc def xyz</html>")
    (let [ii (img-contain file
                          (.getWidth img-compo)   ;; FIX: want size of pane (not size of compo)
                          (.getHeight img-compo))]
      (.setIcon img-compo ii))))

(defn- next!
  [player]
  (draw! @player)
  (swap! player
         (fn [{:keys [ix image-files] :as st}]
           (let [next-ix (mod (inc ix) (count image-files))]
             (assoc st :ix next-ix)))))

(defn stop!
  [player]
  (atat/stop-and-reset-pool! atat-pool))

(defn start!
  [player]
  (stop! player)
  (atat/every 4000
              #(next! player)
              atat-pool
              ; :initial-delay 1000
              ))

(comment
 (stop! nil)
 )
