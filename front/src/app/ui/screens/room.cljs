;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) Andrey Antukh <niwi@niwi.nz>

(ns app.ui.screens.room
  (:require
   [app.store :as st]
   [app.ui.icons :as i]
   [potok.core :as ptk]
   [rumext.alpha :as mf]))

(mf/defc room-screen
  [props]
  [:*
   [:div.profile
    [:div.avatar [:& i/avatar]]
    [:div.greetings "Hi Esther!"]
    [:div.message "Since you're the main blob, you get to start the game when everyone arrives."]
    [:div.button.button-green {:on-click #(st/emit! (ptk/event :nav {:screen :draw}))} "Start game"]]

   [:div.participants
    (for [i (range 4)]
      [:div.participant {:key i}
       [:div.avatar [:& i/avatar]]
       [:div.label (str "name " i)]])]

   [:div.share-link
    [:div.share-link-container
     [:div.label "Share it with friends!"]
     [:div.circle-button
      [:span.label "Copy link"]]]]])




