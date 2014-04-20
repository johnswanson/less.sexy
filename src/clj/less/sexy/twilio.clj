(ns less.sexy.twilio
  (:import com.twilio.sdk.TwilioRestClient
           com.twilio.sdk.TwilioRestException
           com.twilio.sdk.resource.factory.SmsFactory
           com.twilio.sdk.resource.instance.Sms
           com.twilio.sdk.resource.list.SmsList))

(defprotocol ITwilio
  (send-sms [this to body] "Sends an sms to the recipient"))

(defrecord Twilio [sid token number]
  ITwilio
  (send-sms [this to body]
    (let [client (TwilioRestClient. sid token)
          sms-factory (.. client getAccount getSmsFactory)]
      (.create sms-factory (hash-map "From" number "To" to "Body" body)))))

