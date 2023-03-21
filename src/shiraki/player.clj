(ns shiraki.player
  (:require
   [overtone.at-at :as atat]
   [shiraki.util :as u]
   [shiraki.gui :as gui]
   [shiraki.exif :as exif]))


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
                :initial-delay interval)))


;;; State and primitive operations
(defn- st-create!
  [ttl-compo img-compo cmt-compo image-files interval comments]
  (atom {:playing? false
         :interval interval
         :ix nil  ;; what image shown now. only st-play-first! reset this to zero.
         :ttl-compo ttl-compo
         :img-compo img-compo
         :cmt-compo cmt-compo
         :image-files image-files
         :comment-map comments}))
(defn- st-playing?
  [st]
  (:playing? st))
(defn- st-index
  [st]
  (let [ix (:ix st)]
    (assert (some? ix))  ;; ensure st-play-first! is done.
    ix))
(defn- st-interval
  [st]
  (:interval st))
(defn- st-move-ix!
  [player forward?]
  (swap! player
         (fn [{:keys [ix image-files] :as st}]
           (let [new-ix (u/move-ix ix (count image-files) forward?)]
             #_(prn ix new-ix)
             (assoc st :ix new-ix)))))
(defn- st-play-first!
  [player]
  (swap! player assoc
         :ix 0
         :playing? true))
(defn- st-play!
  [player starting?]
  (if (= (st-playing? @player) starting?)
    @player
    (swap! player assoc :playing? starting?)))


;;; Main
(defn- render-title
  [playing? fname n N]
  (let [mark (if playing? "▶ " "■ ")]
    (str mark fname "(" n "/" N ")")))

(defn- render-comment
  [cmt file]
  (str "<html>"
       (or cmt (:user-comment (exif/extract file)))
       "</html>"))

(defn- lookup-comment
  [comment-map fname]
  (let [fname (.toUpperCase fname)]
    (get comment-map fname
         (get comment-map (keyword fname)))))

(defn- draw!
  [{:keys [playing? ix
           ttl-compo img-compo cmt-compo
           image-files comment-map]}]
  #_(println "ix:" ix)
  (try
   (let [file (nth image-files ix)
         fname (.getName file)
         title (render-title
                playing? fname (inc ix) (count image-files))
         cmt (render-comment
              (lookup-comment comment-map fname) file)
         ii (gui/img-contained
             file
             (.getWidth img-compo)   ;; FIX: want size of pane (not size of compo)
             (.getHeight img-compo))]
     (.setText ttl-compo title)
     (.setIcon img-compo ii)
     (.setText cmt-compo cmt))
   (catch Exception ex
     #_(prn ex)
     (.setText ttl-compo "No image found"))))

(defn- tick!
  [player]
  (let [st (st-move-ix! player true)]
    (draw! st)))


;;; API
(defn create!
  [ttl-compo img-compo cmt-compo image-files interval comments]
  (st-create! ttl-compo img-compo cmt-compo image-files interval comments))

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
  (let [st (st-move-ix! player false)]
    (draw! st)
    (when (st-playing? st)
      (timer-start! #(tick! player) (st-interval st)))))

(defn stop!
  [player]
  (timer-stop!)
  (let [st (st-play! player false)]
    (draw! st)))

(defn start!
  [player]
  (let [st (st-play-first! player)]
    (draw! st)
    (timer-start! #(tick! player) (st-interval st))))

(defn suspend!
  [player]
  (when (st-playing? @player)
    (stop! player)))

(defn resume!
  [player]
  (let [st (st-play! player true)]
    (draw! st)
    (timer-start! #(tick! player) (st-interval st))))

(defn toggle-pause!
  [player]
  (if (st-playing? @player)
    (suspend! player)
    (resume! player)))

(comment
 (stop! nil)
 )
