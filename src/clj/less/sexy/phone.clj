(ns less.sexy.phone
  (:require [clojure.string :as string])
  (:import com.google.i18n.phonenumbers.PhoneNumberUtil
           com.google.i18n.phonenumbers.PhoneNumberUtil$PhoneNumberFormat))

(defn str->e164
  "Tries to format a US phone number into the E164 standard.
  Swallows likely errors in parsing! Returns nil for invalid numbers."
  [n]
  (let [u (.. PhoneNumberUtil getInstance)
        number (.parse u n "US")]
    (try
      (when (.isValidNumber u number)
        (.format u number PhoneNumberUtil$PhoneNumberFormat/E164))
      (catch Exception e nil))))
