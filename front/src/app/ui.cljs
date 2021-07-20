;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) Andrey Antukh <niwi@niwi.nz>

(ns app.ui
  (:require
   [lambdaisland.uri :as u]
   [app.config :as cf]
   [app.events]
   [app.store :as st]
   [app.util.spec :as us]
   [app.util.data :as d]
   [app.util.webapi :as wa]
   [app.ui.context :as ctx]
   [app.ui.screens.start :refer [start-screen]]
   [app.ui.screens.room :refer [room-screen]]
   [app.ui.screens.draw :refer [draw-screen]]
   [app.ui.rhooks :as rh]
   [app.util.websockets :as ws]
   [cuerdas.core :as str]
   [goog.events :as events]
   [potok.core :as ptk]
   [rumext.alpha :as mf]))

(declare initialize-websocket)

(mf/defc app
  [props]
  (let [nav         (mf/deref st/nav-ref)
        orientation (rh/use-orientation)]

    ;; (mf/use-effect initialize-websocket)

    [:main.layout
     (if (= :portrait orientation)
       [:div.notice "Please put your smartphone in horizontal position."]
       [:div.screen {:class (str "screen-" (name (:screen nav)))}
        (case (:screen nav)
          "start" [:& start-screen]
          "room" [:& room-screen]
          "draw" [:& draw-screen]
          [:span "not found"])])]))

;; (defn- initialize-websocket
;;   []
;;   (let [
