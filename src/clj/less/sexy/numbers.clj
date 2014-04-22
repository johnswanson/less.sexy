(ns less.sexy.numbers
  (:require [clojure.core.async :as async :refer [go filter< alts! timeout]]
            [less.sexy.twilio :as twilio :refer [send-sms]]
            [less.sexy.storage :as storage]
            [less.sexy.utils :as utils]))


(defn- standardize
  "Tries to turn a phone number into E.164 format"
  [number]
  (format "+%s" (clojure.string/replace number #"[^\d]" "")))

(defn- phone [number] {:number (standardize number)
                      :authorized false
                      :auth-attempts 0
                      :valid :maybe
                      :orig-number number})

(defprotocol IPhoneNumbers
  (authorize [this number] "Authorize a particular number, e.g.
                            (authorize numbers \"+19073511000\")")
  (del [this number] "Marks a number as no longer active")
  (add [this number] "Add a number to the store, e.g.
                            (add numbers \"1-907-35-1-1000)")
  (getp [this number] "Gets the phone object for the number")
  (get-or-create [this number] "Gets or creates a phone object for the number"))

(def authorization-time-limit (* 1000 60))

(defn- is-auth-fn [number code]
  (partial = [:authorize number code]))

(defn- can-authorize [phone]
  (or (nil? phone)
      (not (:authorized phone))
      (:valid phone)
      (>= 5 (:auth-attempts phone))))

(def new-auth-code
  #((utils/rand-str-generator "ABCDEFGHIJKLMNOPQRSTUVWXYZ") 6))

(defrecord PhoneNumbers [store twilio q]
  IPhoneNumbers

  (authorize [this number]
    (if (can-authorize (getp this number))
      (let [phone (storage/inc-auth! store (get-or-create this number))
            code (new-auth-code)
            pred (is-auth-fn number code)
            auth-msg (format "Text back %s to authorize." code)
            auth-chan (filter< pred q)
            timeout-chan (timeout authorization-time-limit)]
        (if (send-sms twilio number auth-msg)
          (go
            (let [[_ ch] (alts! [auth-chan timeout-chan])]
              (when (= ch auth-chan)
                (send-sms twilio number "Yay, you're authorized!!")
                (storage/authorize! store phone))))
          (storage/invalid! store phone)))))

  (del [_ number]
    (storage/del! store number))

  (add [this number]
    (let [phone (get-or-create this number)]
      (storage/add! store phone)
      (authorize this (:number phone))))

  (getp [_ number]
    (storage/getphone store (standardize number)))

  (get-or-create [this number]
    (or (getp this number) (phone number))))

