;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) Andrey Antukh <niwi@niwi.nz>

(ns app.ui
  (:require
   [app.config :as cf]
   [app.store :as st]
   [app.ui.context :as ctx]
   [app.ui.rhooks :as rh]
   [app.ui.screens.draw :refer [draw-screen]]
   [app.ui.screens.end :refer [end-screen]]
   [app.ui.screens.start :refer [start-screen]]
   [app.ui.screens.wait :refer [wait-screen]]
   [app.util.data :as d]
   [app.util.spec :as us]
   [app.util.uuid :as uuid]
   [app.util.webapi :as wa]
   [app.util.websockets :as ws]
   [cljs.core.async :as a]
   [cuerdas.core :as str]
   [goog.events :as events]
   [lambdaisland.uri :as u]
   [potok.core :as ptk]
   [rumext.alpha :as mf]))

(defn get-session-id
  []
  (let [current (.getItem ^js js/sessionStorage "sessionId")]
    (or current
        (let [id (str (uuid/random))]
          (.setItem js/sessionStorage "sessionId" id)
          id))))

(mf/defc game
  [{:keys [state]}]
  (let [{:keys [room screen index]} (:params state)]
    [:div.screen {:class (str "screen-" screen)}
     (cond
       (= "start" screen)
       [:& start-screen {:room room}]

       :else
       (when-let [game (:game state)]
         (cond
           (= "wait" screen)
           [:& wait-screen {:game game}]

           (= "draw" screen)
           [:& draw-screen {:game game}]

           (= "end" screen)
           [:& end-screen {:game game :index index}]

           :else
           [:span "not found"])))]))

(defn- initialize-websocket
  [{:keys [socket] :as wsocket}]
  (letfn [(on-payload [{:keys [data]}]
            (let [game (get data "data")]
              (swap! st/state st/update-game game)))]
    (ws/watch! socket "payload" on-payload)))


(mf/defc app
  [props]
  (let [state       (mf/deref st/state)
        orientation (rh/use-orientation)
        session-id  (mf/use-memo get-session-id)
        wsocket     (rh/use-socket session-id)]

    (mf/use-effect
     (mf/deps wsocket)
     (partial initialize-websocket wsocket))

    [:& (mf/provider ctx/wsocket) {:value wsocket}
     [:& (mf/provider ctx/session-id) {:value session-id}
      [:main.layout
       (cond
         (= :portrait orientation)
         [:div.notice "Please put your smartphone in horizontal position."]

         (false? (:connected wsocket))
         [:div.notice "Connecting..."]

         :else
         [:& game {:state state}])]]]))

