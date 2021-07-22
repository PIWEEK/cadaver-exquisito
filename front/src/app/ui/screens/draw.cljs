;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) Andrey Antukh <niwi@niwi.nz>

(ns app.ui.screens.draw
  (:require
   [app.store :as st]
   [app.util.data :as d]
   [app.ui.icons :as i]
   [app.ui.avatars :refer [avatar]]
   [app.ui.context :as ctx]
   [app.util.webapi :as wa]
   [app.util.websockets :as ws]
   [potok.core :as ptk]
   [goog.events :as gev]
   [rumext.alpha :as mf]))

(def ^:const default-canvas-width 2048)
(def ^:const default-crop-height 100)

(defn draw-line!
  [ctx scale color x1 y1 x2 y2]
  (set! (.-strokeStyle ^js ctx) color)
  (set! (.-lineCap ^js ctx) "round")
  (set! (.-lineWidth ^js ctx) 12)

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

(defn calculate-size
  [node]
  (let [parent    (.-parentNode ^js node)
        ctx       (.getContext ^js node "2d")

        width     (.-clientWidth ^js parent)
        height    (.-clientHeight ^js parent)

        ratio     (/ height width)
        scale     (/ default-canvas-width width)

        canvas-w  default-canvas-width
        canvas-h  (js/Math.round (* canvas-w ratio))]
    {:width width
     :height height
     :ratio ratio
     :scale scale
     :canvas-w canvas-w
     :canvas-h canvas-h}))

(declare clean-drawing!)
(declare draw-prev!)

(defn initialize!
  [node dtool]
  (let [parent    (.-parentNode ^js node)
        ctx       (.getContext ^js node "2d")

        width     (.-clientWidth ^js parent)
        height    (.-clientHeight ^js parent)

        ratio     (/ height width)
        scale     (/ default-canvas-width width)

        canvas-w  default-canvas-width
        canvas-h  (js/Math.round (* canvas-w ratio))

        drawing   (volatile! false)
        origin    (volatile! [0 0])

        on-mouse-down
        (fn [event]
          (js/console.log "mouse-down" event)

          (let [x     (.-offsetX ^js event)
                y     (.-offsetY ^js event)]
            (vreset! drawing true)
            (vreset! origin [x y])))

        on-mouse-move
        (fn [event]
          (when @drawing
            (let [[x1 y1] @origin
                  x2      (.-offsetX ^js event)
                  y2      (.-offsetY ^js event)
                  color   (if (= :pencil (mf/ref-val dtool)) "black" "white")]
              (draw-line! ctx scale color x1 y1 x2 y2)
              (vreset! origin [x2 y2]))))

        on-mouse-up
        (fn [event]
          (js/console.log "mouse-up" event)
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
                (vreset! origin [x y])))))

        on-touch-move
        (fn [event]
          (let [event   (.getBrowserEvent ^js event)
                touches (.-changedTouches event)]
            (when (pos? (alength touches))
              (let [touch   (aget touches 0)
                    [x2 y2] (get-touch-pos touch)
                    tid     (.-identifier ^js touch)

                    [x1 y1] @origin

                    color   (if (= :pencil (mf/ref-val dtool)) "black" "white")]

                (draw-line! ctx scale color x1 y1 x2 y2)
                (vreset! origin [x2 y2])))))

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

    (set! (.-width node) canvas-w)
    (set! (.-height node) canvas-h)

    (fn []
      (doseq [key keys]
        (gev/unlistenByKey key)))))

(defn- image-data->data-uri
  [imgd width height]
  (let [canvas (.createElement ^js js/document "canvas")
        _      (set! (.-width canvas) width)
        _      (set! (.-height canvas) height)
        ctx    (.getContext ^js canvas "2d")]
    (.putImageData ctx imgd 0 0)
    (.toDataURL canvas "image/png")))

(defn- draw-prev!
  [node prev]
  (let [crop-h  default-crop-height

        ctx     (.getContext ^js node "2d")
        duri    (get prev "dataURI")
        img-w   (get prev "width")
        img-h   (get prev "height")
        image   (js/Image.)

        sx      0
        sy      (- img-h crop-h)
        swidth  img-w
        sheight crop-h]

    (set! (.-src image) duri)
    (set! (.-width image) img-w)
    (set! (.-height image) img-h)

    (.addEventListener ^js image "load" #(.drawImage ctx image sx sy swidth sheight 0 0 img-w crop-h))))

(defn- extract-image-data
  [node turn]
  (let [ctx      (.getContext ^js node "2d")
        crop-h   (if (= turn 0) 0 100)

        canvas-w (.-width ^js node)
        canvas-h (.-height ^js node)

        img-w    canvas-w
        img-h    (- canvas-h crop-h)

        img      (.getImageData ctx 0 crop-h img-w img-h)]

    {:data-uri (image-data->data-uri img img-w img-h)
     :width img-w
     :height img-h}))

(defn- clean-drawing!
  [node]
  (let [ctx (.getContext ^js node "2d")]
    (.clearRect ^js ctx 0 0 (.-width node), (.-height node))))

(mf/defc draw-screen
  {::mf/wrap [mf/memo]}
  [{:keys [game]}]
  (let [session-id (mf/use-ctx ctx/session-id)
        wsock      (mf/use-ctx ctx/wsocket)
        msgbus     (mf/use-ctx ctx/msgbus)

        canvas     (mf/use-ref)

        turn       (get game "activeCanvasTurn")
        last?      (get game "isLastCanvasTurn")
        players    (get game "players")

        player     (d/seek #(= session-id (get % "playerId")) players)

        render-cnt (mf/use-state 0)
        wait       (mf/use-state nil)
        dtool      (mf/use-ref :pencil)

        crop-h    (mf/use-state 100)

        select-drawtool
        (fn [tool]
          (mf/set-ref-val! dtool tool)
          (swap! render-cnt inc))

        finish-turn
        (mf/use-callback
         (mf/deps turn wsock)
         (fn [event]
           (let [node    (mf/ref-val canvas)
                 imgd    (extract-image-data node turn)
                 socket  (:socket wsock)
                 params  {:room (get game "room")
                          :dataURI (:data-uri imgd)
                          :canvasWidth (:width imgd)
                          :canvasHeight (:height imgd)}]

             (reset! wait turn)
             (ws/send! socket "sendCanvas" params))))
        ]

    (mf/use-layout-effect
     #(let [node (mf/ref-val canvas)]
        (initialize! node dtool)))

    (mf/use-effect
     (mf/deps game)
     #(let [node      (mf/ref-val canvas)
            parent    (.-parentNode ^js node)
            width     (.-clientWidth ^js parent)
            scale     (/ default-canvas-width width)]
        (compare-and-set! crop-h 100 (/ 100 scale))))

    (mf/use-layout-effect
     (mf/deps turn)
     (fn []
       (let [turn     (str (get game "activeCanvasTurn"))
             prev-idx (get-in game ["canvasTurns" session-id turn 1])
             prev     (get-in game ["canvas" prev-idx])
             node     (mf/ref-val canvas)]
         (clean-drawing! node)
         (when prev (draw-prev! node prev))
         (constantly nil))))

    ;; (cljs.pprint/pprint (dissoc game "canvas"))

    [:*
     (when (= @wait turn)
       [:div.notice-overlay
        [:span.message "Waiting next turn..."]])
     [:div.header]
     [:div.main-content
      [:div.left-sidebar
       [:div.logo [:img {:src "/images/logo.svg"}]]
       [:div.connection-status]
       [:div.spacer]
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
        [:canvas {:ref (fn [node]
                         (when node
                           (js/console.log "changed canvas node" (hash node))
                           (mf/set-ref-val! canvas node)))}]
        [:div.top-overlay {:style {:height (str @crop-h "px")}}]
        [:div.bottom-overlay {:style {:height (str @crop-h "px")}}]]]
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
     [:div.footer]]))

