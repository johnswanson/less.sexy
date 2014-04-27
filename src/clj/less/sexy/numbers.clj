(ns less.sexy.numbers
  (:require [clojure.core.async :as async :refer [go filter< alts! timeout close! chan go-loop]]
            [less.sexy.twilio :as twilio :refer [send-sms]]
            [less.sexy.storage :as storage]
            [less.sexy.utils :as utils]
            [less.sexy.phone :refer [str->e164]]))

(defn- phone
  "Returns a phone record, or nil if unable to parse number."
  [number]
  (if-let [formatted (str->e164 number)]
    {:number formatted
     :authorized false
     :auth-attempts 0
     :valid :maybe
     :orig-number number}))

(defprotocol IPhoneNumbers
  (authorize [this number] "Authorize a particular number, e.g.
                            (authorize numbers \"+19073511000\")")
  (del [this number] "Marks a number as no longer active")
  (add [this number] "Add a number to the store, e.g.
                            (add numbers \"1-907-35-1-1000)")
  (auth-chan [this number] "Returns a newly created channel to hold
                           authorization attempts for the number")
  (close-auth-chan [this number] "Closes the authorization channel for the
                                  number")
  (getp [this number] "Gets the phone object for the number")
  (get-or-create [this number] "Gets or creates a phone object for the number"))

(def authorization-time-limit (* 1000 60))

(defn- can-authorize [phone]
  (and (:number phone)
       (not (:authorized phone))
       (:valid phone)
       (>= 5 (:auth-attempts phone))))

(def ^:private new-auth-code
  #((utils/rand-str-generator "ABCDEFGHIJKLMNOPQRSTUVWXYZ") 6))

(defrecord PhoneNumbers [store twilio chans]
  IPhoneNumbers

  (auth-chan [this number]
    (let [c (chan)]
      (swap! chans assoc-in [:pending-authorizations number] c)
      c))

  (close-auth-chan [this number]
    (swap! chans update-in [:pending-authorizations] dissoc number))

  (authorize [this number]
    (if (can-authorize (getp this number))
      (let [phone (storage/inc-auth! store (get-or-create this number))
            code (new-auth-code)
            auth-msg (format "Text back %s to authorize." code)
            attempt-chan (auth-chan this number)
            [auth-success auth-fail] (async/split
                                           (partial = [number code])
                                           attempt-chan)
            timeout-chan (timeout authorization-time-limit)]
        (if (send-sms twilio number auth-msg)
          (go-loop []
            (let [[_ ch] (alts! [auth-success auth-fail timeout-chan])]
              (condp = ch
                auth-success (do (send-sms twilio number "You're authorized!")
                                 (close-auth-chan this number)
                                 (storage/authorize! store phone))
                auth-fail (do (send-sms twilio number "Sorry, auth failed") (recur))
                timeout-chan (close-auth-chan this number))))
          (storage/invalid! store phone)))))

  (del [_ number]
    (storage/del! store number))

  (add [this number]
    (when-let [phone (get-or-create this number)]
      (storage/add! store phone)
      (authorize this (:number phone))))

  (getp [_ number]
    (storage/getphone store (str->e164 number)))

  (get-or-create [this number]
    (or (getp this number) (phone number))))

