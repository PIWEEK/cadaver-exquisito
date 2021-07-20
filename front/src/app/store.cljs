;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) Andrey Antukh <niwi@niwi.nz>

(ns app.store
  (:require
   [app.util.data :as d]
   [app.util.spec :as us]
   [app.util.storage :refer [storage]]
   [app.util.timers :as tm]
   [app.util.webapi :as wa]
   [beicon.core :as rx]
   [cljs.spec.alpha :as s]
   [cuerdas.core :as str]
   [lambdaisland.glogi :as log]
   [lambdaisland.uri :as u]
   [okulary.core :as l]
   [potok.core :as ptk]))

(log/set-level 'app.store :info)

(defonce state (l/atom {:nav {:screen "start"}}))

(def nav-ref
  (l/derived :nav state))

(def message-ref
  (l/derived :message state))

(add-watch nav-ref ::router
           (fn [_ _ oval nval]
             (when (not= oval nval)
               (let [query (u/map->query-string nval)
                     uri   (-> (wa/get-current-uri)
                               (assoc :query query))]
                 (.pushState ^js js/history #js {} "" (str uri))))))


;; --- Helper functions

(defn nav!
  [params]
  (swap! state assoc :nav params))

