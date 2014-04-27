(ns less.sexy.twilio
  (:require [pandect.core :refer [sha1-hmac-bytes]]
            [clojure.data.codec.base64 :as b64])
  (:import com.twilio.sdk.TwilioRestClient
           com.twilio.sdk.TwilioRestException
           com.twilio.sdk.resource.factory.SmsFactory
           com.twilio.sdk.resource.instance.Sms
           com.twilio.sdk.resource.list.SmsList))

(defprotocol ITwilio
  (send-sms [this to body] "Sends an sms to the recipient")
  (valid-twilio-request? [this req] "Makes sure Twilio sent this request"))

(defrecord Twilio [sid token number]
  ITwilio
  (send-sms [this to body]
    (let [client (TwilioRestClient. sid token)
          sms-factory (.. client getAccount getSmsFactory)]
      (try
        (.create sms-factory (hash-map "From" number "To" to "Body" body))
        (catch TwilioRestException e nil))))

  (valid-twilio-request? [_ req]
    (let [hash-in (format "%s://%s%s%s"
                          (name (:scheme req))
                          (get-in req [:headers "host"])
                          (:uri req)
                          (->> (:form-params req)
                            (sort-by key)
                            (map #(str (key %) (val %)))
                            (apply str)))]
      (= (String. (b64/encode (sha1-hmac-bytes hash-in token)))
         (get-in req [:headers "x-twilio-signature"])))))
