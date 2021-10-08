;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) Andrey Antukh <niwi@niwi.nz>

(ns app.ui.screens.draw
  (:require
   [app.store :as st]
   [app.ui.avatars :refer [avatar get-player-color]]
   [app.ui.common :as cm]
   [app.ui.context :as ctx]
   [app.ui.icons :as i]
   [app.util.data :as d]
   [app.util.object :as obj]
   [app.util.timers :as ts]
   [app.util.webapi :as wa]
   [app.util.websockets :as ws]
   [goog.events :as gev]
   [potok.core :as ptk]
   [rumext.alpha :as mf]))

(def ^:const default-canvas-width 2048)
(def ^:const default-crop-height 100)
(def ^:const default-turn-seconds 60)

(defn draw-line!
  [ctx {:keys [scale color x1 y1 x2 y2 line-width]
        :or {line-width 12}}]

  (obj/set! ctx "strokeStyle" color)
  (obj/set! ctx "lineCap" "round")
  (obj/set! ctx "lineWidth" line-width)

  (let [x1 (* x1 scale)
        y1 (* y1 scale)
        x2 (* x2 scale)
        y2 (* y2 scale)]
    (.beginPath ^js ctx)
    (.moveTo ^js ctx x1 y1)
    (.lineTo ^js ctx x2 y2)
    (.stroke ^js ctx)
    (.fill ^js ctx)
    (.closePath ^js ctx)))

(defn- draw-previous-drawing!
  [node data]
  (when data
    (let [crop-h  default-crop-height

          ctx     (wa/get-context node "2d")
          duri    (get data "dataURI")
          img-w   (get data "width")
          img-h   (get data "height")

          sx      0
          sy      (- img-h crop-h)
          swidth  img-w
          sheight crop-h

          image   (-> (js/Image.)
                      (obj/set! "src" duri)
                      (obj/set! "width" img-w)
                      (obj/set! "height" img-h))]

      (gev/listenOnce image "load"
                      #(.drawImage ctx image sx sy swidth sheight 0 0 img-w crop-h)))))

(defn initialize!
  [node dtool]
  (let [parent    (.-parentNode ^js node)
        ctx       (wa/get-context node "2d")

        width     (.-clientWidth ^js parent)
        height    (.-clientHeight ^js parent)

        ratio     (/ height width)
        scale     (/ default-canvas-width width)

        canvas-w  default-canvas-width
        canvas-h  (js/Math.round (* canvas-w ratio))

        drawing   (volatile! false)
        origin    (volatile! {:x1 0 :y1 0})

        on-mouse-down
        (fn [event]
          (let [x1    (obj/get event "offsetX")
                y1    (obj/get event "offsetY")]
            (vreset! drawing true)
            (vreset! origin {:x1 x1 :y1 y1})))

        with-draw-tool
        (fn [specs]
          (let [draw-tool (mf/ref-val dtool)]
            (cond-> specs
              (= :pencil draw-tool)
              (assoc :color "black")

              (= :erraser draw-tool)
              (->
               (assoc :color "white")
               (assoc :line-width 18)))))

        on-mouse-move
        (fn [event]
          (when @drawing
            (let [x2    (obj/get event "offsetX")
                  y2    (obj/get event "offsetY")
                  specs (-> @origin
                            (assoc :scale scale)
                            (assoc :x2 x2)
                            (assoc :y2 y2)
                            (with-draw-tool))]
              (draw-line! ctx specs)
              (vreset! origin {:x1 x2 :y1 y2}))))

        on-mouse-up
        (fn [event]
          (vreset! drawing false))

        get-touch-pos
        (fn [touch]
          (let [brect (.getBoundingClientRect node)]
            [(- (.-clientX ^js touch)
                (.-x ^js brect)
                (.-offsetLeft ^js node) 0)
             (- (.-clientY ^js touch)
                (.-y ^js brect)
                (.-offsetTop ^js node) 0)]))

        on-touch-start
        (fn [event]
          (let [event   (.getBrowserEvent ^js event)
                touches (.-changedTouches event)]
            (when (pos? (alength touches))
              (let [touch  (aget touches 0)
                    [x y]  (get-touch-pos touch)
                    tid    (.-identifier ^js touch)]
                (vreset! drawing true)
                (vreset! origin {:x1 x :y1 y})))))

        on-touch-move
        (fn [event]
          (let [event   (.getBrowserEvent ^js event)
                touches (.-changedTouches event)]
            (when (pos? (alength touches))
              (let [touch   (aget touches 0)
                    [x2 y2] (get-touch-pos touch)
                    tid     (.-identifier ^js touch)
                    spec    (-> @origin
                                (with-draw-tool)
                                (assoc :scale scale)
                                (assoc :x2 x2)
                                (assoc :y2 y2))]
                (draw-line! ctx spec)
                (vreset! origin {:x1 x2 :y1 y2})))))

        on-touch-end
        (fn [event]
          (vreset! drawing false))

        keys [(gev/listen node "mousedown" on-mouse-down)
              (gev/listen node "pointerdown" wa/capture-pointer)
              (gev/listen node "pointerup" wa/release-pointer)
              (gev/listen node "mousemove" on-mouse-move)
              (gev/listen node "mouseup" on-mouse-up)
              (gev/listen node "touchstart" on-touch-start)
              (gev/listen node "touchmove" on-touch-move)
              (gev/listen node "touchend" on-touch-end)]
        ]

    (obj/set! node "width" canvas-w)
    (obj/set! node "height" canvas-h)

    (fn []
      (doseq [key keys]
        (gev/unlistenByKey key)))))

(defn- image-data->data-uri
  [imgd width height]
  (let [canvas (wa/create-element "canvas" {:width width :height height})
        ctx    (wa/get-context canvas "2d")]
    (.putImageData ^js ctx imgd 0 0)
    (.toDataURL ^js canvas "image/png")))

(defn- extract-image-data
  [node turn]
  (let [ctx      (wa/get-context node "2d")
        crop-h   (if (= turn 0) 0 100)

        canvas-w (.-width ^js node)
        canvas-h (.-height ^js node)

        img-w    canvas-w
        img-h    (- canvas-h crop-h)

        img      (.getImageData ^js ctx 0 crop-h img-w img-h)]

    {:data-uri (image-data->data-uri img img-w img-h)
     :width img-w
     :height img-h}))

(defn- clean-drawing!
  [node]
  (let [ctx (wa/get-context node "2d")]
    (obj/set! ctx "fillStyle" "#ffffff")
    (.fillRect ^js ctx 0 0 (.-width node), (.-height node))))

(mf/defc draw-screen
  {::mf/wrap [mf/memo]}
  [{:keys [game]}]
  (let [session-id (mf/use-ctx ctx/session-id)
        wsock      (mf/use-ctx ctx/wsocket)
        canvas     (mf/use-ref)

        help       (mf/use-ref)

        turn       (get game "activeCanvasTurn")
        last?      (get game "isLastCanvasTurn")
        players    (get game "players")
        canvas-id  (get-in game ["canvasTurns" session-id (str turn) 0])

        player     (d/seek #(= session-id (get % "playerId")) players)

        render-cnt (mf/use-state 0)
        wait       (mf/use-state nil)
        dtool      (mf/use-ref :pencil)

        crop-h     (mf/use-state 100)

        progress   (mf/use-state 0)

        select-drawtool
        (fn [tool]
          (mf/set-ref-val! dtool tool)
          (swap! render-cnt inc))

        finish-turn
        (mf/use-callback
         (mf/deps turn wsock canvas-id)
         (fn [event]
           (let [node    (mf/ref-val canvas)
                 imgd    (extract-image-data node turn)

                 socket  (:socket wsock)
                 params  {:room (get game "room")
                          :dataURI (:data-uri imgd)
                          :canvasID canvas-id
                          :canvasWidth (:width imgd)
                          :canvasHeight (:height imgd)}]

             (reset! wait turn)
             (ws/send! socket "sendCanvas" params))))
        ]

    (mf/use-layout-effect
     (mf/deps player)
     (fn []
       (when player
         (let [root  (.-documentElement ^js js/document)
               style (.-style ^js root)
               color (get-player-color player)]
           (.setProperty ^js style "--main-color" color)))))


    (mf/use-layout-effect
     (mf/deps turn)
     (fn []
       (ts/schedule 10000 (fn []
                            (when-let [node (mf/ref-val help)]
                              (.add (.-classList ^js node) "help-hide"))))))


    (mf/use-layout-effect
     #(let [node (mf/ref-val canvas)]
        (initialize! node dtool)))

    (mf/use-layout-effect
     (mf/deps turn)
     (fn []
       (let [prev-idx (get-in game ["canvasTurns" session-id (str turn) 1])
             prev     (get-in game ["canvas" prev-idx])
             node     (mf/ref-val canvas)]
         (clean-drawing! node)
         (draw-previous-drawing! node prev)
         (constantly nil))))

    (mf/use-effect
     (mf/deps game)
     #(let [node      (mf/ref-val canvas)
            parent    (.-parentNode ^js node)
            width     (.-clientWidth ^js parent)
            scale     (/ default-canvas-width width)]
        (compare-and-set! crop-h 100 (/ 100 scale))))

    (mf/use-effect
     (mf/deps turn)
     (fn []
       (letfn [(on-tick [n]
                 (let [v (/ (* n 100.0) default-turn-seconds)]
                   (reset! progress v)
                   (cond
                     (= n default-turn-seconds)
                     (reset! wait turn)
                     (> n default-turn-seconds)
                     (do
                       (finish-turn)
                       ::ts/stop))))]
         (ts/interval 1000 on-tick))))

    [:*
     (when (= @wait turn)
       [:div.notice-overlay
        (if last?
          [:span.message "Finalizing the game..."]
          [:span.message "Waiting next turn..."])])

     [:div.header]
     [:div.main-content
      (cond
        last?
        [:div.help-overlay.bottom {:ref help}
         [:div.content
          "This is the last drawing. Let it breath at the bottom."]]

        (= 0 turn)
        [:div.help-overlay {:ref help}
         [:div.content
          [:span "Time to start your drawing!"]
          [:span "Make sure you go all the way til the bottom edges."]]])

      [:& cm/left-sidebar {}
       [:div.draw-buttons
        [:div.button.finish-turn
         {:on-click finish-turn
          :title "Finish turn"}]
        [:div.button {:class (when (= (mf/ref-val dtool) :pencil) "selected")
                      :on-click #(select-drawtool :pencil)}
         [:& i/lapiz]]
        [:div.button {:class (when (= (mf/ref-val dtool) :erraser) "selected")
                      :on-click #(select-drawtool :erraser)}
         [:& i/goma]]]]
      [:div.main-panel
       [:div.draw-panel
        [:canvas {:ref canvas}]
        (when (pos? turn)
          [:div.top-overlay {:style {:height (str @crop-h "px")}}])
        (when-not last?
          [:div.bottom-overlay {:style {:height (str @crop-h "px")}}])]]
      [:div.right-sidebar
       [:div.participants
        (for [player players]
          (when (not= (get player "playerId") session-id)
            [:div.participant {:key (get player "playerId")}
             [:div.avatar [:& avatar {:profile player}]]
             #_[:div.label (get player "name")]]))

        [:div.participant
         [:div.avatar [:& avatar {:profile player}]]]]

       [:div.greetings
        (str "Hi " (get player "name"))]]]
     [:div.footer
      [:div.progress-bar
       [:div.position {:style {:width (str @progress "vw")}}]]
      ]
     ]))
