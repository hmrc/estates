/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package utils

import models.EstateRegistration
import play.api.libs.json.JsValue

trait JsonRequests extends JsonUtils {
  lazy val invalidEstateRegistrationJson: String = getJsonFromFile("mdtp/invalid-estate-registration-01.json")

  lazy val estateRegRequest: EstateRegistration =
    getJsonValueFromFile("mdtp/valid-estate-registration-01.json").validate[EstateRegistration].get

  lazy val estateRegistration01: String = getJsonFromFile("mdtp/valid-estate-registration-01.json")
  lazy val estateRegistration03: String = getJsonFromFile("mdtp/valid-estate-registration-03.json")

  lazy val validEstateVariationsRequestJson: String = getJsonFromFile("mdtp/valid-estate-variation-api.json")

  lazy val estateVariationsRequest: JsValue =
    getJsonValueFromFile("mdtp/valid-estate-variation-api.json").validate[JsValue].get

  lazy val invalidEstateVariationsRequestJson: String = getJsonFromFile("etmp/invalid-estate-variation-api.json")
  lazy val invalidEstateVariationsRequest: JsValue    = getJsonValueFromFile("etmp/invalid-estate-variation-api.json")

  lazy val get4MLDEstateResponse: JsValue = getJsonValueFromFile("etmp/valid-get-estate-4mld-response.json")

  lazy val desGet5MLDEstateResponseJson: String = getJsonFromFile("etmp/des-valid-get-estate-5mld-response.json")
  lazy val hipGet5MLDEstateResponseJson: String = getJsonFromFile("etmp/hip-valid-get-estate-5mld-response.json")
  lazy val get5MLDEstateResponse: JsValue       = getJsonValueFromFile("etmp/des-valid-get-estate-5mld-response.json")

  lazy val getTransformedEstateResponse: JsValue = getJsonValueFromFile(
    "transformed/variations/valid-get-estate-response-transformed.json"
  )

  lazy val getTransformedPersonalRepResponse: JsValue = getJsonValueFromFile(
    "transformed/variations/valid-get-estate-response-transformed-personal-rep-only.json"
  )

  lazy val desGetEstateInvalidResponseJson: JsValue = getJsonValueFromFile(
    "etmp/des-valid-get-estate-invalid-response.json"
  )

  lazy val hipGetEstateInvalidResponseJson: JsValue = getJsonValueFromFile(
    "etmp/hip-valid-get-estate-invalid-response.json"
  )

  lazy val get4MLDEstateExpectedResponse: JsValue = getJsonValueFromFile(
    "mdtp/valid-get-estate-expected-4mld-response.json"
  )

  lazy val get5MLDEstateExpectedResponse: JsValue = getJsonValueFromFile(
    "mdtp/valid-get-estate-expected-5mld-response.json"
  )

  lazy val desGetTrustOrEstateProcessingResponseJson: String = getJsonFromFile(
    "etmp/des-valid-get-trust-or-estate-in-processing-response.json"
  )

  lazy val hipGetTrustOrEstateProcessingResponseJson: String = getJsonFromFile(
    "etmp/hip-valid-get-trust-or-estate-in-processing-response.json"
  )

  lazy val getTrustOrEstateProcessingResponse: JsValue = getJsonValueFromFile(
    "etmp/des-valid-get-trust-or-estate-in-processing-response.json"
  )

  lazy val desGetTrustOrEstatePendingClosureResponseJson: String = getJsonFromFile(
    "etmp/des-valid-get-trust-or-estate-pending-closure-response.json"
  )

  lazy val hipGetTrustOrEstatePendingClosureResponseJson: String = getJsonFromFile(
    "etmp/hip-valid-get-trust-or-estate-pending-closure-response.json"
  )

  lazy val getTrustOrEstatePendingClosureResponse: JsValue = getJsonValueFromFile(
    "etmp/des-valid-get-trust-or-estate-pending-closure-response.json"
  )

  lazy val desGetTrustOrEstateClosedResponseJson: String = getJsonFromFile(
    "etmp/des-valid-get-trust-or-estate-closed-response.json"
  )

  lazy val hipGetTrustOrEstateClosedResponseJson: String = getJsonFromFile(
    "etmp/hip-valid-get-trust-or-estate-closed-response.json"
  )

  lazy val getTrustOrEstateClosedResponse: JsValue = getJsonValueFromFile(
    "etmp/des-valid-get-trust-or-estate-closed-response.json"
  )

  lazy val desGetTrustOrEstateSuspendedResponseJson: String = getJsonFromFile(
    "etmp/des-valid-get-trust-or-estate-suspended-response.json"
  )

  lazy val hipGetTrustOrEstateSuspendedResponseJson: String = getJsonFromFile(
    "etmp/hip-valid-get-trust-or-estate-suspended-response.json"
  )

  lazy val desGetTrustOrEstateSuspendedResponse: JsValue = getJsonValueFromFile(
    "etmp/des-valid-get-trust-or-estate-suspended-response.json"
  )

  lazy val hipGetTrustOrEstateSuspendedResponse: JsValue = getJsonValueFromFile(
    "etmp/hip-valid-get-trust-or-estate-suspended-response.json"
  )

  lazy val desGetTrustOrEstateParkedResponseJson: String = getJsonFromFile(
    "etmp/des-valid-get-trust-or-estate-parked-response.json"
  )

  lazy val hipGetTrustOrEstateParkedResponseJson: String = getJsonFromFile(
    "etmp/hip-valid-get-trust-or-estate-parked-response.json"
  )

  lazy val getTrustOrEstateParkedResponse: JsValue = getJsonValueFromFile(
    "etmp/des-valid-get-trust-or-estate-parked-response.json"
  )

  lazy val desGetTrustOrEstateObsoletedResponseJson: String = getJsonFromFile(
    "etmp/des-valid-get-trust-or-estate-obsoleted-response.json"
  )

  lazy val hipGetTrustOrEstateObsoletedResponseJson: String = getJsonFromFile(
    "etmp/hip-valid-get-trust-or-estate-obsoleted-response.json"
  )

  lazy val getTrustOrEstateObsoletedResponse: JsValue = getJsonValueFromFile(
    "etmp/des-valid-get-trust-or-estate-obsoleted-response.json"
  )

}
