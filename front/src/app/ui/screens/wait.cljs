;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) Andrey Antukh <niwi@niwi.nz>

(ns app.ui.screens.wait
  (:require
   [app.store :as st]
   [app.ui.context :as ctx]
   [app.ui.icons :as i]
   [app.ui.avatars :refer [avatar]]
   [app.util.data :as d]
   [app.util.webapi :as wa]
   [app.util.websockets :as ws]
   [lambdaisland.uri :as u]
   [potok.core :as ptk]
   [rumext.alpha :as mf]))

(mf/defc wait-screen
  [{:keys [game]}]
  (let [session-id (mf/use-ctx ctx/session-id)
        wsock      (mf/use-ctx ctx/wsocket)

        waiting    (mf/use-state false)

        players    (get game "players")
        room       (get game "room")

        profile    (d/seek #(= session-id (get % "playerId")) players)
        admin      (d/seek #(get % "isAdmin") players)

        fullname   (get profile "name")
        is-admin?  (get profile "isAdmin")
        avatar-id  (get profile "avatar")

        on-submit
        (fn []
          (let [socket (:socket wsock)]
            (reset! waiting true)
            (ws/send! socket "startGame" {})))]

    (if (true? @waiting)
      [:div.notice "joining game...."]
      [:*
       [:div.profile
        [:div.avatar [:& avatar {:profile profile}]]
        [:div.greetings (str "Hi " (get profile "name") "!")]
        (if is-admin?
          [:div.message "Since you're the main blob, you get to start the game when everyone arrives."]
          [:div.message (str (get admin "name") " will start the game once evryone arrives, so let’s wait for a bit")])
        (when is-admin?
          [:div.button.button-green {:on-click on-submit} "Start game"])]

       [:div.participants
        (for [player players]
          (when (not= (get player "playerId") session-id)
            [:div.participant {:key (get player "playerId")}
             [:div.avatar [:& avatar {:profile player}]]
             [:div.label (get player "name")]]))]

       (when is-admin?
         (let [query (u/map->query-string {:room room :screen "start"})
               href (-> (wa/get-current-uri)
                        (assoc :query query))]

           [:div.share-link
            [:div.share-link-container
             [:div.label "Share it with friends!"]
             [:div.circle-button
              [:a.label {:href (str href)} "Copy link"]]]]))])))




