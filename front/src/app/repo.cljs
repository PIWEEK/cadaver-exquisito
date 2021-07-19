;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) Andrey Antukh <niwi@niwi.nz>

(ns app.repo
  (:require
   [app.util.data :as d]
   [app.util.http :as http]
   [app.util.object :as obj]
   [app.util.spec :as us]
   [app.util.storage :refer [storage cache]]
   [app.util.time :as dt]
   [beicon.core :as rx]
   [cljs.spec.alpha :as s]
   [cuerdas.core :as str]
   [lambdaisland.glogi :as log]
   [lambdaisland.uri :as u]))

(log/set-level 'app.repo :trace)

;; --- HELPERS

(declare handle-response)

(def base-uri (u/uri "https://backend"))

(defn- handle-response
  [response]
  (cond
    (http/success? response)
    (rx/of (:body response))

    (= (:status response) 400)
    (rx/throw (:body response))

    (= (:status response) 401)
    (rx/throw {:type :authentication
               :code :not-authenticated})

    (= (:status response) 404)
    (rx/throw {:type :not-found :code :object-not-found})

    :else
    (rx/throw {:type :internal-error
               :status (:status response)
               :body (:body response)})))

(defn- default-params
  []
  {:token (:app.events/token storage)})

(defmulti request (fn [id params opts] id))

;; --- PUBLIC API

(defn req!
  ([id] (req! id {} {}))
  ([id params] (req! id params {}))
  ([id params options] (request id params options)))

;; --- IMPL

;; TODO
