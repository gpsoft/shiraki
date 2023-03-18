(ns shiraki.player
  (:require
   [overtone.at-at :as atat])
  (:import
   [java.awt Image]
   [javax.swing ImageIcon]))

(defonce atat-pool (atat/mk-pool))

(defn create!
  [img-compo txt-compo image-files]
  (atom {:ix 0
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
  [{:keys [ix img-compo txt-compo image-files]}]
  (println "ix:" ix)
  (let [file (nth image-files ix)
        fname (.getName file)
        ii (img-contain file (.getWidth img-compo) (.getHeight img-compo))
        ]
    (.setIcon img-compo ii)
    (.setText txt-compo fname)))

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
