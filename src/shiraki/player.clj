(ns shiraki.player
  (:require
   [overtone.at-at :as atat])
  (:import
   [javax.swing ImageIcon]))

(defonce atat-pool (atat/mk-pool))

(defn create!
  [img-compo txt-compo image-files]
  (atom {:ix 0
         :img-compo img-compo
         :txt-compo txt-compo
         :image-files image-files}))

(defn- draw!
  [{:keys [ix img-compo txt-compo image-files]}]
  (let [file (nth image-files ix)
        fname (.getName file)
        ii (new ImageIcon fname)
        ; i (.getImage ii)
        ; ii (new ImageIcon (.getScaledInstance i 300 300 Image/SCALE_SMOOTH))
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
  (atat/every 5000
              #(next! player)
              atat-pool
              ; :initial-delay 4000
              ))

(comment
 (stop! nil)
 )
