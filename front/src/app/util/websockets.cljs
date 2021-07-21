;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) UXBOX Labs SL

(ns app.util.websockets
  "SocketIO interface with backend."
  (:require
   ["socket.io-client" :as io]
   [goog.events :as ev]
   [app.util.webapi :as wa]
   [cljs.core.async :as a]))

(def uri
  (wa/get-current-uri))

      ;; (assoc :query "/socket")))

(defn connect
  [session-id]
  (let [socket (io #js {:query #js {:playerID session-id}
                        ;; :path "/api/socket.io"
                        ;; :transports #js ["polling"]
                        })]
    socket))

(defn send!
  [socket event data]
  (prn "ws/send!" event (clj->js data))
  (.emit ^js socket event (clj->js data)))

(defn watch!
  [socket ename callback]
  (.on ^js socket ename (fn [payload]
                          (callback {::data payload :data (js->clj payload)})))
  socket)

(defn close!
  [socket]
  (.close ^js socket))
