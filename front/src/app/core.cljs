;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) Andrey Antukh <niwi@niwi.nz>

(ns app.core
  (:require
   [lambdaisland.uri :as u]
   [lambdaisland.glogi :as log]
   [lambdaisland.glogi.console :as glogi-console]
   [promesa.core :as p]
   [rumext.alpha :as mf]
   [app.store :as st]
   [app.ui :as ui]
   [app.util.webapi :as wa]))

(glogi-console/install!)
(enable-console-print!)

(defn start
  [& args]
  (log/info :msg "initializing")
  (mf/mount (mf/element ui/app)
            (wa/get-element "app")))

(defn stop
  [done]
  ;; an empty line for visual feedback of restart
  (js/console.log "")
  (log/info :msg "stoping")
  (done))

(defn restart
  []
  (mf/unmount (wa/get-element "app"))
  (mf/unmount (wa/get-element "modal"))
  (start))

(defn ^:dev/after-load after-load
  []
  (restart))
