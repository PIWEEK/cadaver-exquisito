;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) Andrey Antukh <niwi@niwi.nz>

(ns app.ui.screens.start
  (:require
   [app.store :as st]
   [app.ui.icons :as i]
   [potok.core :as ptk]
   [rumext.alpha :as mf]))

(mf/defc start-screen
  [props]
  [:*
   [:div.title "Welcome to cadaver exquisito!"]
   [:div.form
    [:input {:type "text"
             :placeholder "Enter your name..."
             :default-value ""}]]

   [:div.actions
    [:div.button.button-main {:on-click #(st/nav! {:screen :room})}
     "Create a new game"]]])
