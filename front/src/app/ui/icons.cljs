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

(mf/defc lapiz
  []
  [:svg {:viewBox "0 0 30 30"}
   [:path {:d "M14.9924 5.06153C12.5105 5.05902 11.3739 5.92409 10.9868 6.31404C10.8777 6.42341 10.8257 6.56989 10.8257 6.72309L10.8402 19.1482C10.8407 19.6481 10.9863 20.1368 11.2581 20.556L14.5063 25.5603C14.7405 25.9205 15.2684 25.9207 15.502 25.561L18.755 20.5656C19.0289 20.1441 19.1751 19.6515 19.1743 19.1493L19.159 6.72863C19.1587 6.5678 19.0962 6.41682 18.9813 6.30383C18.5818 5.90534 17.4419 5.0627 14.9924 5.06153ZM18.0689 19.5923L16.8092 21.5407C16.76 21.6171 16.6681 21.651 16.5812 21.6251C16.1472 21.5018 15.6722 21.4122 15.0186 21.4118C14.3655 21.4122 13.8903 21.5008 13.4562 21.6224C13.369 21.6465 13.278 21.6131 13.2287 21.5363L11.9721 19.5869L11.9725 18.598C11.9725 18.598 12.9205 17.6879 15.0219 17.6892C17.1209 17.691 18.0695 18.602 18.0695 18.602L18.0689 19.5923Z"}]])

(mf/defc goma
  []
  [:svg {:viewBox "0 0 30 30"}
   [:path {:d "M17.1061 6.80171L12.8916 6.76085C11.7261 6.74909 10.7628 7.69389 10.7495 8.85922L10.7985 21.9502C10.7861 23.1149 11.7288 24.0772 12.8943 24.089L17.1079 24.1305C18.2735 24.1422 19.2368 23.1974 19.25 22.0321L19.2019 8.94045C19.2143 7.77576 18.2716 6.81347 17.1061 6.80171ZM17.9989 21.1006L17.9788 22.0191C17.9758 22.2827 17.8507 22.5268 17.6358 22.6887C17.5309 22.7678 17.3559 22.8612 17.1209 22.8592L12.9073 22.8177C12.683 22.8157 12.4726 22.7248 12.3131 22.5625C12.1536 22.4001 12.0677 22.1876 12.0704 21.9641L12.0906 21.0456C12.0936 20.782 12.218 20.537 12.4338 20.3744C12.5387 20.2954 12.7137 20.202 12.9487 20.204L17.1631 20.2448C17.3875 20.2469 17.5978 20.3377 17.7573 20.5001C17.9151 20.6637 18.001 20.8762 17.9989 21.1006Z"}]])
