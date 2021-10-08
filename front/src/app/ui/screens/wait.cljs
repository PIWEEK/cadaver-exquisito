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
   [app.ui.avatars :refer [avatar get-player-color]]
   [app.ui.common :as cm]
   [app.util.data :as d]
   [app.util.webapi :as wa]
   [app.util.websockets :as ws]
   [lambdaisland.uri :as u]
   [potok.core :as ptk]
   [rumext.alpha :as mf]))

(defn- build-share-link
  [room]
  (let [query (u/map->query-string {:room room :screen "start"})]
    (-> (wa/get-current-uri)
        (assoc :query query)
        (str))))


(mf/defc wait-screen
  [{:keys [game]}]
  (let [session-id (mf/use-ctx ctx/session-id)
        wsock      (mf/use-ctx ctx/wsocket)

        waiting    (mf/use-state false)

        players    (get game "players")
        room       (get game "room")

        player    (d/seek #(= session-id (get % "playerId")) players)
        admin      (d/seek #(get % "isAdmin") players)

        fullname   (get player "name")
        is-admin?  (get player "isAdmin")
        avatar-id  (get player "avatar")

        on-submit
        (fn []
          (let [socket (:socket wsock)]
            (reset! waiting true)
            (ws/send! socket "startGame" {})))

        slink (build-share-link room)

        on-copy-link
        (fn [event]
          (wa/prevent-default! event)
          (when-let [clipboard (unchecked-get js/navigator "clipboard")]
            (.writeText ^js clipboard slink)))]


    (mf/use-layout-effect
     (mf/deps player)
     (fn []
       (when player
         (let [root  (.-documentElement ^js js/document)
               style (.-style ^js root)
               color (get-player-color player)]
           (.setProperty ^js style "--main-color" color)))))



    [:*
     (when (true? @waiting)
      [:div.notice-overlay "joining game...."])

     [:div.main-content
      [:& cm/left-sidebar {}]
      [:div.main-panel
       [:div.profile
        [:div.avatar [:& avatar {:profile player}]]
        [:div.greetings (str "Hi " (get player "name") "!")]
        (if is-admin?
          [:div.message "Since you're the main blob, you get to start the game when everyone arrives. Enjoy!"]
          [:div.message (str (get admin "name") " will start the game once everyone arrives, stand by!")])
        (when is-admin?
          [:div.button.button-start {:on-click on-submit} "Start game"])]

       [:div.participants
        (for [player players]
          (when (not= (get player "playerId") session-id)
            [:div.participant {:key (get player "playerId")}
             [:div.avatar [:& avatar {:profile player}]]
             [:div.label (get player "name")]]))]]

      [:div.share-link
       [:div.share-link-container
        [:div.label "Share it with friends!"]
        [:div.circle-button
         [:a.label {:href slink
                    :on-click on-copy-link}
          "Copy link"]]]]]]))




