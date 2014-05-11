(ns less.sexy.numbers
  (:require [org.httpkit.server :refer [send!]]
            [clojure.core.async :as async :refer [go filter< alts! timeout close! chan go-loop]]
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
  (auth-received [this number code] "Notifies that an authorization attempt was
                                    received.")
  (del [this number] "Marks a number as no longer active")
  (add [this number] "Add a number to the store, e.g.
                            (add numbers \"1-907-35-1-1000)")
  (auth-chan [this number] "Returns a newly created channel to hold
                           authorization attempts for the number")
  (fwd-sms [this number body] "Forwards sms to the number's partner")
  (getp [this number] "Gets the phone object for the number")
  (get-or-create [this number] "Gets or creates a phone object for the number")
  (auth-pending? [this number] "Returns true if the number has a pending auth"))

(def authorization-time-limit (* 1000 60))

(defn- can-authorize [phone]
  (and (:number phone)
       (not (:authorized phone))
       (:valid phone)
       (>= 5 (:auth-attempts phone))))

(def ^:private new-auth-code
  #((utils/rand-str-generator "ABCDEFGHIJKLMNOPQRSTUVWXYZ") 6))

(defn- auth-chan! [this number]
  (let [c (chan)]
    (swap! (:chans this) assoc-in [:pending-authorizations number] c)
    c))

(defn- close-auth-chan! [this number]
  (swap! (:chans this) update-in [:pending-authorizations] dissoc number))

(defn- open-auth-chan [p phone success-handler prompt success failure]
  (let [code (new-auth-code)
        attempt-chan (auth-chan! p (:number phone))
        [auth-success auth-fail] (async/split
                                   (partial = [(:number phone) code])
                                   attempt-chan)
        timeout-chan (timeout authorization-time-limit)]
    (if (send-sms (:twilio p) (:number phone) (format prompt code))
      (go-loop []
        (let [[_ ch] (alts! [auth-success auth-fail timeout-chan])]
          (condp = ch
            auth-success (do (send-sms (:twilio p) (:number phone) success)
                             (close-auth-chan! p (:number phone))
                             (send! ((->
                                       @(:chans p)
                                       :pending-authorization-chans)
                                     (:number phone))
                                    "authorized")
                             (swap! (:chans p)
                                    update-in
                                    [:pending-authorization-chans]
                                    dissoc
                                    (:number phone))
                             (success-handler))
            auth-fail (do (send-sms (:twilio p) (:number phone) failure) (recur))
            timeout-chan (close-auth-chan! p (:number phone))))))))

(defrecord PhoneNumbers [store twilio chans]
  IPhoneNumbers

  (auth-chan [this number] (get-in @chans [:pending-authorizations number]))

  (authorize [this number]
    (when (can-authorize (getp this number))
      (when-not (open-auth-chan
                  this
                  (storage/inc-auth! store (getp this number))
                  #(storage/authorize! store (getp this number))
                  "Text back %s to authorize!"
                  "You're authorized!"
                  "Authorization failed, try again.")
        (storage/invalid! store (getp this number)))))

  (del [this number]
    (when (:authorized (getp this number))
      (open-auth-chan
        this
        (storage/inc-auth! store (getp this number))
        #(storage/del! store (getp this number))
        "Text back %s to deauthorize!"
        "Successfully deauthorized!"
        "Authorization failed, try again.")))

  (auth-received [this number code]
    (if-let [c (auth-chan this number)]
      (go (>! c [number code]))))

  (add [this number]
    (when-let [phone (get-or-create this number)]
      (storage/add! store phone)
      (authorize this (:number phone))
      phone))

  (auth-pending? [this number]
    (not (nil? (auth-chan this number))))

  (fwd-sms [this number body]
    (let [phone (getp this number)
          partner (getp this (:partner phone))]
      (when partner
        (send-sms twilio (:partner phone) body))))

  (getp [_ number]
    (storage/getphone store (str->e164 number)))

  (get-or-create [this number]
    (or (getp this number) (phone number))))

