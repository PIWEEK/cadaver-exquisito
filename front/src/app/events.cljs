;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) Andrey Antukh <niwi@niwi.nz>

(ns app.events
  (:require
   [lambdaisland.glogi :as log]
   [lambdaisland.uri :as u]
   [beicon.core :as rx]
   [cljs.spec.alpha :as s]
   [cuerdas.core :as str]
   [okulary.core :as l]
   [potok.core :as ptk]
   [app.repo :as rp]
   [app.events.messages :as em]
   [app.util.data :as d]
   [app.util.webapi :as wa]
   [app.util.exceptions :as ex]
   [app.util.spec :as us]
   [app.util.storage :refer [storage]]
   [app.util.time :as dt]
   [app.util.transit :as t]))

(log/set-level 'app.events :trace)

(def re-throw #(rx/throw %))

(defmethod ptk/resolve :initialize
  [_ params]
  (ptk/reify :initialize
    ptk/WatchEvent
    (watch [_ state stream]
      )))


(s/def :app.events$nav/section ::us/keyword)
(s/def :app.events$nav/token ::us/string)

(s/def ::nav
  (s/keys :opt-un [:app.events$nav/section
                   :app.events$nav/token]))

(defmethod ptk/resolve :nav
  [_ params]
  (ptk/reify :nav
    IDeref
    (-deref [_] params)

    ptk/UpdateEvent
    (update [_ state]
      (-> state
          (update :nav (fn [nav]
                         (reduce-kv (fn [res k v]
                                      (if (nil? v)
                                        (dissoc res k)
                                        (assoc res k v)))
                                    nav
                                    params)))
          (update :nav #(us/conform ::nav %))))

    ptk/EffectEvent
    (effect [_ state stream]
      (let [uri   (wa/get-current-uri)
            nav   (s/unform ::nav (:nav state))
            query (u/map->query-string nav)
            uri   (assoc uri :query query)]
        (.pushState ^js js/history #js {} "" (str uri))))))
