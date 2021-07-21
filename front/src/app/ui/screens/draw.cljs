;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) Andrey Antukh <niwi@niwi.nz>

(ns app.ui.screens.draw
  (:require
   [app.store :as st]
   [app.ui.icons :as i]
   [app.ui.context :as ctx]
   [cljs.core.async :as a]
   [potok.core :as ptk]
   [goog.events :as gev]
   [rumext.alpha :as mf]))

(def ^:const default-canvas-width 2048)

(defn draw-line!
  [ctx scale x1 y1 x2 y2]
  (set! (.-strokeStyle ^js ctx) "black")
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
  [node]
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
                  y2      (.-offsetY ^js event)]
              (draw-line! ctx scale x1 y1 x2 y2)
              (vreset! origin [x2 y2]))))

        on-mouse-up
        (fn [event]
          (vreset! drawing false)
          (prn "on-mouse-up"))

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
          (prn "on-touch-start")
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
          (prn "on-touch-move")
          (let [event   (.getBrowserEvent ^js event)
                touches (.-changedTouches event)]
            (when (pos? (alength touches))


              (let [touch   (aget touches 0)
                    [x2 y2] (get-touch-pos touch)
                    tid     (.-identifier ^js touch)

                    [x1 y1] @origin]

                (draw-line! ctx scale x1 y1 x2 y2)
                (vreset! origin [x2 y2])))))

        on-touch-end
        (fn [event]
          (prn "on-mouse-end")
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


(defn- clean-drawing!
  [node]
  (let [ctx (.getContext ^js node "2d")]
    (.clearRect ^js ctx 0 0 (.-width node), (.-height node))))

(mf/defc draw-screen
  {::mf/wrap [mf/memo]}
  [{:keys [game]}]
  (let [canvas (mf/use-ref)
        msgbus (mf/use-ctx ctx/msgbus)

        turn   (get game "activeCanvasTurn")
        last?  (get game "isLastCanvasTurn")
        wait   (mf/use-state nil)]

    (mf/use-effect
     #(initialize! (mf/ref-val canvas)))

    (mf/use-effect
     (mf/deps turn)
     (fn []
       (prn "initialize turn" turn)
       (let [cch (a/chan 1)]
         (a/go
           (let [[msg port] (a/alts! [msgbus cch])]
             (when (= port msgbus)
               (reset! wait turn))))

         (clean-drawing! (mf/ref-val canvas))
         (fn []
           (a/close! cch)))))

    (cljs.pprint/pprint game)

    [:*
     (when @wait
       [:div.notice-overlay
        [:span.message "Waiting next turn..."]])
     [:div.header]
     [:div.main-content
      [:div.left-sidebar]
      [:div.main-panel
       [:div.draw-panel
        [:canvas {:ref canvas}]
        [:div.top-overlay {:style {:height "50px"}}]
        [:div.bottom-overlay {:style {:height "50px"}}]]]
      [:div.right-sidebar]]
     [:div.footer]]))

