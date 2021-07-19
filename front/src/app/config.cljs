;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) Andrey Antukh <niwi@niwi.nz>

(ns app.config
  (:require
   [clojure.spec.alpha :as s]
   [cuerdas.core :as str]
   [lambdaisland.uri :as u]))


(def public-uri
  (let [uri (u/uri (.-origin ^js js/location))]
    ;; Ensure that the path always ends with "/"; this ensures that
    ;; all path join operations works as expected.
    (cond-> uri
      (not (str/ends-with? (:path uri) "/"))
      (update :path #(str % "/")))))


