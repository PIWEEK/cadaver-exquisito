;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) Andrey Antukh <niwi@niwi.nz>

(ns app.ui.icons
  (:require
   [app.store :as st]
   [potok.core :as ptk]
   [rumext.alpha :as mf]))

(mf/defc avatar
  []
  [:svg {:viewBox "0 0 53 53"}
   [:path {:d "M0.574707 53.0991L52.7737 53.0991L52.7737 26.9996C52.7737 12.5853 41.0886 0.900131 26.6742 0.900131C12.2599 0.900131 0.574707 12.5853 0.574707 26.9996L0.574707 53.0991Z" :fill "#8AAE7E"}]])

