;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) Andrey Antukh <niwi@niwi.nz>

(ns app.ui.screens.draw
  (:require
   [app.store :as st]
   [app.ui.icons :as i]
   [potok.core :as ptk]
   [rumext.alpha :as mf]))

(mf/defc draw-screen
  [props]
  [:div.draw-screen
   [:div.left-panel ""]
   [:div.main-panel
    [:div.draw-panel
     [:canvas {:width "2048" :height "1080"}]]]

   [:div.right-panel ""]])

