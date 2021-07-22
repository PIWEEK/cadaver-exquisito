;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) Andrey Antukh <niwi@niwi.nz>

(ns app.store
  (:require
   [app.util.data :as d]
   [app.util.spec :as us]
   [app.util.uuid :as uuid]
   [app.util.timers :as tm]
   [app.util.webapi :as wa]
   [beicon.core :as rx]
   [cljs.spec.alpha :as s]
   [cuerdas.core :as str]
   [lambdaisland.glogi :as log]
   [lambdaisland.uri :as u]
   [okulary.core :as l]
   [rumext.alpha :as mf]
   [potok.core :as ptk]))

(log/set-level 'app.store :info)

(defn get-session-id
  []
  (let [current (.getItem ^js js/sessionStorage "sessionId")]
    (or current
        (let [id (str (uuid/random))]
          (.setItem js/sessionStorage "sessionId" id)
          id))))


(defn get-initial-state
  []
  (let [params (-> (:query (wa/get-current-uri))
                   (u/query-string->map))]
    {:params (or params {:screen "start"})}))

(defonce state (l/atom (get-initial-state)))

(add-watch state ::router
           (fn [_ _ oval nval]
             (when (not= (:params oval) (:params nval))
               (let [query (u/map->query-string (:params nval))
                     uri   (-> (wa/get-current-uri)
                               (assoc :query query))]
                 (.pushState ^js js/history #js {} "" (str uri))))))


;; --- Helper functions

(defn nav!
  [params]
  (swap! state assoc :params params))

(defn update-game
  [state game origin]
  (-> state
      (update :params (fn [params]
                        (if game
                          (let [status (get game "status")
                                params (assoc params :room (get game "room"))]
                            (cond-> params
                              (= status "waiting")  (assoc :screen "wait")
                              (= status "ongoing")  (assoc :screen "draw")
                              (= status "finished") (assoc :screen "end")))
                          (assoc params :screen "start"))))
      (assoc :game game)))
