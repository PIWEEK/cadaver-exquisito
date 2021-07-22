;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) Andrey Antukh <niwi@niwi.nz>

(ns app.ui.avatars
  (:require
   [app.store :as st]
   [app.ui.icons :as i]
   [app.ui.context :as ctx]
   [cljs.core.async :as a]
   [potok.core :as ptk]
   [goog.events :as gev]
   [rumext.alpha :as mf]))

(def paths
  {"S0" "M43.3521 58.3936C62.7513 42.8404 62.7513 17.6236 43.3521 2.07038C42.555 1.43133 41.7376 0.818533 40.9015 0.231995C40.0654 0.818532 39.248 1.43133 38.4509 2.07038C19.0517 17.6236 19.0517 42.8404 38.4509 58.3936C39.248 59.0327 40.0654 59.6455 40.9015 60.232C41.7376 59.6455 42.555 59.0327 43.3521 58.3936Z"
   "S1" "M40.5991 0.231992C40.5991 0.231992 60.599 23.7058 80.599 30.232C60.5989 36.7582 40.5989 60.232 40.5989 60.232C40.5989 60.232 20.599 36.7582 0.598946 30.232C20.5989 23.7058 40.5991 0.231992 40.5991 0.231992Z"
   "S2" "M40.9015 0.231995L44.2527 15.5496L53.918 3.20293L50.2913 18.4576L64.3564 11.5273L54.4701 23.6977L70.1493 23.5564L55.9615 30.232L70.1493 36.9076L54.4701 36.7663L64.3564 48.9367L50.2913 42.0064L53.918 57.2611L44.2527 44.9144L40.9015 60.232L37.5503 44.9144L27.885 57.2611L31.5117 42.0064L17.4466 48.9367L27.3329 36.7663L11.6537 36.9076L25.8415 30.232L11.6537 23.5564L27.3329 23.6977L17.4466 11.5273L31.5117 18.4576L27.885 3.20293L37.5503 15.5496L40.9015 0.231995Z"
   "S3" "M20.599 20.232C20.599 9.1863 29.5533 0.231995 40.599 0.231995C51.6447 0.231995 60.599 9.1863 60.599 20.232V40.232C60.599 51.2777 51.6447 60.232 40.599 60.232C29.5533 60.232 20.599 51.2777 20.599 40.232V20.232Z"
   "S4" "M15.599 0.231995C7.31473 0.231995 0.598999 6.94772 0.598999 15.232C0.598999 23.5163 7.31473 30.232 15.599 30.232C7.31473 30.232 0.598999 36.9477 0.598999 45.232C0.598999 53.5163 7.31473 60.232 15.599 60.232H65.599C73.8833 60.232 80.599 53.5163 80.599 45.232C80.599 36.9477 73.8833 30.232 65.599 30.232C73.8833 30.232 80.599 23.5163 80.599 15.232C80.599 6.94772 73.8833 0.231995 65.599 0.231995L15.599 0.231995Z"
   "S5" "M40.599 0.231995L51.2056 19.6254L70.599 30.232L51.2056 40.8386L40.599 60.232L29.9924 40.8386L10.599 30.232L29.9924 19.6254L40.599 0.231995Z"
   "S6" "M40.9011 -0.332039L60.901 19.668L60.901 39.668L40.901 59.668L20.901 39.668L20.901 19.668L40.9011 -0.332039Z"
   "S7" "M66.599 16.8914V43.9385L40.599 60.232L14.599 43.5726V16.5255L40.599 0.231995L66.599 16.8914Z"
   "S8" "M70.901 30C70.901 46.5685 57.4695 60 40.901 60C24.3325 60 10.901 46.5685 10.901 30C10.901 13.4315 24.3325 0 40.901 0C57.4695 0 70.901 13.4315 70.901 30Z"
   "S9" "M40.599 0.231995L47.4777 9.06148L58.2326 5.96148L58.6077 17.1479L69.1307 20.9615L62.859 30.232L69.1307 39.5025L58.6077 43.3161L58.2326 54.5025L47.4777 51.4025L40.599 60.232L33.7203 51.4025L22.9654 54.5025L22.5903 43.3161L12.0673 39.5025L18.339 30.232L12.0673 20.9615L22.5903 17.1479L22.9654 5.96148L33.7203 9.06148L40.599 0.231995Z"
   "S10" "M10.599 30.232L40.5989 60.232L70.599 30.232L40.5991 0.231991L10.599 30.232Z"
   "S11" "M2.72961 60.232L78.4682 60.232L40.5991 0.23198L2.72961 60.232Z"
   "S12" "M10.599 60.232L70.599 60.232L70.599 30.232C70.599 13.6635 57.1675 0.231991 40.599 0.231991C24.0305 0.231991 10.599 13.6635 10.599 30.232L10.599 60.232Z"})

(def animations
  {"A0" "hvr-float"
   "A1" "hvr-buzz"
   "A2" "hvr-rotate-left"
   "A3" "hvr-wobble-horizontal"
   "A4" "hvr-forward"
   "A5" "hvr-buzz-out"})

(def colors
  {"c0" "#FFBA53"
   "c1" "#C1C34F"
   "c2" "#8AAE7E"
   "c3" "#55B5EB"
   "c4" "#AFA0FF"
   "c5" "#F392BA"
   "c6" "#FF78C1"
   "c7" "#FF7171"
   "c8" "#F39958"
   "c9" "#FFBA53"})

(defn on-mouse-move
  [container-ref ball-ref-1 ball-ref-2 event]
  (let [cnt (mf/ref-val container-ref)
        brt (.getBoundingClientRect ^js cnt)

        ww  (.-innerWidth js/window)
        wh  (.-innerHeight js/window)

        fw  (- (/ ww 2)
               (.-x brt)
               (/ 82 2))

        fh  (- (/ wh 2)
               (.-y brt)
               (/ 60 2))

        cx  (.-clientX ^js event)
        cy  (.-clientY ^js event)

        x  (/ (* (+ cx fw) 100) ww)
        y  (/ (* (+ cy fh) 100) wh)

        s1 (.-style (mf/ref-val ball-ref-1))
        s2 (.-style (mf/ref-val ball-ref-2))

        f  15
        x  (if (<= x f) f x)
        y  (if (<= y f) f y)]

    (set! (.-left s1) (str x "%"))
    (set! (.-top s1) (str y "%"))
    (set! (.-left s2) (str x "%"))
    (set! (.-top s2) (str y "%"))))


(mf/defc eyes
  {::mf/wrap [mf/memo]}
  []
  (let [container-ref (mf/use-ref)
        ball-ref-1    (mf/use-ref)
        ball-ref-2    (mf/use-ref)]

    (mf/use-effect
     #(let [f   (partial on-mouse-move container-ref ball-ref-1 ball-ref-2)
            key (gev/listen js/window "mousemove" f)]
        (fn [] (gev/unlistenByKey key))))

    [:div.eyes {:ref container-ref}
     [:div.eye
      [:div.ball {:ref ball-ref-1}]]
     [:div {:class "eye"}
      [:div.ball {:ref ball-ref-2}]]]))

(mf/defc avatar
  {::mf/wrap [mf/memo]}
  [{:keys [profile]}]
  (let [[symbol color animation] (get profile "avatar")
        klass (get animations animation)
        dpath (get paths symbol)
        color (get colors color)]

    [:div.player-avatar {:class klass}
     [:& eyes]
     [:svg {:viewBox "0 0 82 60"}
      [:path {:d dpath :fill color}]]]))


