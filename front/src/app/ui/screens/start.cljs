;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) Andrey Antukh <niwi@niwi.nz>

(ns app.ui.screens.start
  (:require
   [app.store :as st]
   [app.ui.icons :as i]
   [app.ui.context :as ctx]
   [app.util.uuid :as uuid]
   [app.util.webapi :as wa]
   [app.util.websockets :as ws]
   [cuerdas.core :as str]
   [potok.core :as ptk]
   [rumext.alpha :as mf]))

(mf/defc start-screen
  [{:keys [room]}]
  (let [pname   (mf/use-state "")
        wsock   (mf/use-ctx ctx/wsocket)
        waiting (mf/use-state false)

        on-submit
        (fn [event]
          (let [socket (:socket wsock)
                params {:room (or room (str (uuid/next)))
                        :name @pname
                        :tabID (:session-id wsock)}]
            (reset! waiting true)
            (ws/send! socket "joinGame" params)))]

    [:*
     (if (true? @waiting)
       [:div.notice "joining game...."]
       [:*
        [:div.title "Welcome to cadaver exquisito!"]
        [:div.form
         [:input {:type "text"
                  :placeholder "Enter your name..."
                  :value @pname
                  :on-change #(let [val (-> (wa/get-target %)
                                            (wa/get-value))]
                                (reset! pname val))}]]

        [:div.actions
         (when-not (str/blank? @pname)
           [:div.button.button-main {:on-click on-submit}
            (if room
              "Join game"
              "Create a new game")])]])]))

