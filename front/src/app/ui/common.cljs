;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) Andrey Antukh <niwi@niwi.nz>

(ns app.ui.common
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

(mf/defc left-sidebar
  [{:keys [children]}]
  (let [session-id (mf/use-ctx ctx/session-id)
        wsock      (mf/use-ctx ctx/wsocket)
        on-exit    (fn []
                     (let [socket (:socket wsock)]
                       (ws/send! socket "leaveGame" {})
                       (swap! st/state (fn [state]
                                         (-> state
                                             (dissoc :game)
                                             (assoc :params {:screen "start"}))))))]
    [:div.left-sidebar
     [:div.logo {:on-click on-exit} [:img {:src "/images/logo.svg"}]]
     [:div.connection-status]
     [:div.spacer]
     children]))
