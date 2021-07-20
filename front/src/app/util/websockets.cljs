;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) UXBOX Labs SL

(ns app.util.websockets
  "A interface to webworkers exposed functionality."
  (:require
   [goog.events :as ev]
   [cljs.core.async :as a])
  (:import
   goog.net.WebSocket
   goog.net.WebSocket.EventType))

(defn open
  [uri]
  (let [ws  (WebSocket. #js {:autoReconnect true})
        in  (a/chan 128)
        out (a/chan 128)
        cls (a/chan 1)
        res (a/chan 1)

        on-message
        (fn [message]
          (a/offer! in {::type :msg ::data message}))

        on-error
        (fn [error]
          (a/offer! in {::type :err ::data error}))

        on-open
        (fn [event]
          (a/offer! out {:type "hello" :session-id 1}))

        kys [(ev/listen ws EventType.MESSAGE #(on-message (.-message ^js %)))
             (ev/listen ws EventType.ERROR on-error)
             (ev/listen ws EventType.OPENED on-open)]]

    (.open ws (str uri))

    (a/go
      (a/<! cls)
      (a/close! in)
      (a/close! out)
      (.close ^js ws)
      (run! ev/unlistenByKey kys))

    (a/go-loop []
      (let [[val port] (a/alts! [cls out])]
        (when (= port out)
          (.send ^js ws val)
          (recur))))

    {::in in
     ::out out
     ::cls cls}))

