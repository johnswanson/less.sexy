(ns less.sexy.numbers
  (:require [clojure.core.async :as async :refer [go filter< alts! timeout]]
            [less.sexy.twilio :as twilio :refer [send-sms]]
            [less.sexy.storage :as storage]
            [less.sexy.utils :as utils]))

;; things a number can do, or can be done with a number
;;
;; - text partner
;; - request authorization:
;;    - text the number
;;    - launch go block to authorize number when
;; - store a number
;;    - request authorization
;;    - add to database
;; - delete a number
;;    - make inactive
;; - get partner number
;; - block partner number

(defprotocol IPhoneNumbers
  (authorize [this number] "Authorize a particular number, e.g.
                            (authorize numbers \"+19073511000\")")
  (del-number [this number] "Marks a number as no longer active")
  (add-number [this number] "Add a number to the store, e.g.
                            (add-number numbers \"1-907-35-1-1000)"))

(def authorization-time-limit (* 1000 60))

(defn is-auth-fn [number code]
  (partial = [:authorize number code]))

(def new-auth-code
  #((utils/rand-str-generator "ABCDEFGHIJKLMNOPQRSTUVWXYZ") 6))

(defrecord PhoneNumbers [store twilio q]
  IPhoneNumbers

  (authorize [_ number]
    (let [code (new-auth-code)
          pred (is-auth-fn number code)
          auth-msg (format "Welcome! Text back %s to authorize." code)
          auth-chan (filter< pred q)
          timeout-chan (timeout authorization-time-limit)]
      (send-sms twilio number auth-msg)
      (go
        (let [[_ ch] (alts! [auth-chan timeout-chan])]
          (when (= ch auth-chan)
            (send-sms twilio number "Yay, you're authorized!!")
            (storage/authorize! store number))))))

  (del-number [_ number]
    (storage/del-number! store number))

  (add-number [this number]
    (storage/add-number! store number)
    (authorize this number)))

