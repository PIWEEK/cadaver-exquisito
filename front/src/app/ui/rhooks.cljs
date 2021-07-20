;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) Andrey Antukh <niwi@niwi.nz>

(ns app.ui.rhooks
  "A collection of custom React Hooks."
  (:require
   [app.util.data :as d]
   [app.util.timers :as ts]
   [app.util.webapi :as wa]
   [goog.events :as events]
   [rumext.alpha :as mf]))

(defn- use-orientation
  []
  (let [orientation (mf/use-state (wa/get-orientation))]
    (mf/use-effect
     (fn []
       (let [key (events/listen js/screen.orientation "change"
                                (fn [event]
                                  (ts/schedule 200 #(reset! orientation (wa/get-orientation)))))]
         (fn []
           (events/unlistenByKey key)))))

        @orientation))

