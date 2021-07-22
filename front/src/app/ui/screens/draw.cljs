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
   [app.util.websockets :as ws]
   [potok.core :as ptk]
   [goog.events :as gev]
   [rumext.alpha :as mf]))

(def ^:const default-canvas-width 2048)

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

(defn initialize!
  [node dtool]
  (let [parent  (.-parentNode ^js node)
        ctx     (.getContext ^js node "2d")

        width   (.-clientWidth ^js parent)
        height  (.-clientHeight ^js parent)
        ratio   (/ height width)

        cwidth  default-canvas-width
        cheight (js/Math.round (* cwidth ratio))

        drawing (volatile! false)
        origin  (volatile! [0 0])

        scale   (/ default-canvas-width width)

        on-mouse-down
        (fn [event]
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
              (gev/listen node "mousemove" on-mouse-move)
              (gev/listen node "mouseup" on-mouse-up)
              (gev/listen node "touchstart" on-touch-start)
              (gev/listen node "touchmove" on-touch-move)
              (gev/listen node "touchend" on-touch-end)]]

    (set! (.-width node) cwidth)
    (set! (.-height node) cheight)

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

        wait       (mf/use-state nil)
        dtool      (mf/use-state :pencil)
        dtool*     (mf/use-ref :pencil)

        select-drawtool
        (fn [tool]
          (mf/set-ref-val! dtool* tool)
          (reset! dtool tool))

        finish-turn
        (fn [event]
          (let [node    (mf/ref-val canvas)
                ctx     (.getContext ^js node "2d")

                cropf   (if (= turn 0) 0 50)

                cwidth  (.-width ^js node)
                cheight (.-height ^js node)

                imgdw   cwidth
                imgdh   (- cheight cropf)

                imgd    (.getImageData ctx 0 cropf imgdw imgdh)
                duri    (image-data->data-uri imgd imgdw imgdh)

                socket  (:socket wsock)]
            (reset! wait turn)
            (ws/send! socket "sendCanvas" {:room (get game "room")
                                           :dataURI duri
                                           :canvasWidth imgdw
                                           :canvasHeight imgdh})))
        ]
    (mf/use-effect
     (fn []
       (let [node (mf/ref-val canvas)]
         (initialize! node dtool*))))

    (mf/use-effect
     (mf/deps turn)
     (fn []
       (clean-drawing! (mf/ref-val canvas))
       (constantly nil)))

    ;; (cljs.pprint/pprint players)

    [:*
     (when @wait
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
        [:div.button {:class (when (= @dtool :pencil) "selected")
                      :on-click #(select-drawtool :pencil)}
         [:& i/lapiz]]
        [:div.button {:class (when (= @dtool :erraser) "selected")
                      :on-click #(select-drawtool :erraser)}
         [:& i/goma]]]]
      [:div.main-panel
       [:div.draw-panel
        [:canvas {:ref canvas}]
        [:div.top-overlay {:style {:height "50px"}}]
        [:div.bottom-overlay {:style {:height "50px"}}]]]
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

