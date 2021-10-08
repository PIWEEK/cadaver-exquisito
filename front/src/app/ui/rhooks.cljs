;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) Andrey Antukh <niwi@niwi.nz>

(ns app.ui.rhooks
  "A collection of custom React Hooks."
  (:require
   [app.util.data :as d]
   [app.util.timers :as ts]
   [app.util.webapi :as wa]
   [app.util.websockets :as ws]
   [goog.events :as events]
   [rumext.alpha :as mf]))

(defn- use-orientation
  []
  (let [orientation (mf/use-state (wa/get-orientation))]
    (mf/use-effect
     (fn []
       ;; If we have window.screen.orientation, use the most efficient
       ;; way to detect the screen orientation change; but if we dont
       ;; have it, proceed to use the old technique of polling.
       (if (and (exists? js/screen)
           (exists? (.-orientation js/screen)))
         (let [key (events/listen js/screen.orientation "change"
                                  (fn [event]
                                    (ts/schedule 200 #(reset! orientation (wa/get-orientation)))))]
           (fn [] (events/unlistenByKey key)))
         (letfn [(on-poll []
                   (reset! orientation (wa/get-orientation)))]
           (ts/interval 1000 on-poll)))))
    @orientation))

(defn use-socket
  [session-id]
  (let [socket    (mf/use-memo (mf/deps session-id) #(ws/connect session-id))
        connected (mf/use-state false)]

    (mf/use-effect
     (mf/deps socket)
     (fn []
       (ws/watch! socket "connect" (fn [_] (reset! connected (.-connected ^js socket))))
       (ws/watch! socket "disconnect" (fn [_] (reset! connected (.-connected ^js socket))))
       (ws/watch! (.-io socket) "error" (fn [e] (js/console.log "ERROR" e)))
       (fn []
         (ws/close! socket))))

    (mf/use-memo
     (mf/deps @connected socket)
     (fn []
       {:connected @connected
        :session-id session-id
        :socket socket}))))


