/*
 * Copyright 2020 HM Revenue & Customs
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

package connectors

import exceptions._
import models.ExistingCheckResponse._
import models._
import models.getEstate._
import models.variation.{VariationFailureResponse, VariationSuccessResponse}
import play.api.http.Status._
import play.api.libs.json.{JsError, Json}
import utils.ErrorResponses._
import utils.JsonRequests

class DesConnectorSpec extends BaseConnectorSpec with JsonRequests {

  lazy val connector: DesConnector = injector.instanceOf[DesConnector]

  lazy val request: ExistingCheckRequest = ExistingCheckRequest("trust name", postcode = Some("NE65TA"), "1234567890")

  def createTrustOrEstateEndpoint(utr: String) = s"/trusts/registration/$utr"

  ".checkExistingEstate" should {

    "return Matched" when {
      "estate data match with existing estate" in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(server, "/estates/match", requestBody, OK, """{"match": true}""")

        val futureResult = connector.checkExistingEstate(request)

        whenReady(futureResult) {
          result => result mustBe Matched
        }
      }
    }
    "return NotMatched" when {
      "estate data does not with existing estate" in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(server, "/estates/match", requestBody, OK, """{"match": false}""")

        val futureResult = connector.checkExistingEstate(request)

        whenReady(futureResult) {
          result => result mustBe NotMatched
        }
      }
    }

    "return BadRequest" when {
      "payload sent is not valid" in {
        val wrongPayloadRequest = request.copy(utr = "NUMBER1234")
        val requestBody = Json.stringify(Json.toJson(wrongPayloadRequest))

        stubForPost(server, "/estates/match", requestBody, BAD_REQUEST, Json.stringify(jsonResponse400))

        val futureResult = connector.checkExistingEstate(wrongPayloadRequest)

        whenReady(futureResult) {
          result => result mustBe BadRequest
        }
      }
    }

    "return AlreadyRegistered" when {
      "estate is already registered with provided details" in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(server, "/estates/match", requestBody, CONFLICT, Json.stringify(jsonResponseAlreadyRegistered))

        val futureResult = connector.checkExistingEstate(request)

        whenReady(futureResult) {
          result => result mustBe AlreadyRegistered
        }
      }
    }

    "return ServiceUnavailable" when {
      "DES dependent service is not responding" in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(server, "/estates/match", requestBody, SERVICE_UNAVAILABLE, Json.stringify(jsonResponse503))

        val futureResult = connector.checkExistingEstate(request)

        whenReady(futureResult) {
          result => result mustBe ServiceUnavailable
        }
      }
    }

    "return ServerError" when {
      "DES is experiencing some problem" in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(server, "/estates/match", requestBody, INTERNAL_SERVER_ERROR, Json.stringify(jsonResponse500))

        val futureResult = connector.checkExistingEstate(request)

        whenReady(futureResult) {
          result => result mustBe ServerError
        }
      }
    }

    "return ServerError" when {
      "des is returning forbidden response" in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(server, "/estates/match", requestBody, CONFLICT, "{}")

        val futureResult = connector.checkExistingEstate(request)

        whenReady(futureResult) {
          result => result mustBe ServerError
        }
      }
    }
  }

  ".registerEstate" should {

    "return TRN" when {
      "valid request to DES register an estate" in {
        val requestBody = Json.stringify(Json.toJson(estateRegRequest)(EstateRegistration.estateRegistrationWriteToDes))

        stubForPost(server, "/estates/registration", requestBody, OK, """{"trn": "XTRN1234567"}""")

        val futureResult = connector.registerEstate(estateRegRequest)

        whenReady(futureResult) {
          result => result mustBe RegistrationTrnResponse("XTRN1234567")
        }

      }
    }

    "return BadRequest response" when {
      "payload sent to DES is invalid" in {
        val requestBody = Json.stringify(Json.toJson(estateRegRequest)(EstateRegistration.estateRegistrationWriteToDes))
        stubForPost(server, "/estates/registration", requestBody, BAD_REQUEST, Json.stringify(jsonResponse400))

        val futureResult = connector.registerEstate(estateRegRequest)

        whenReady(futureResult) {
          result => result mustBe RegistrationFailureResponse(BAD_REQUEST)
        }
      }
    }

    "return AlreadyRegisteredResponse" when {
      "estates is already registered with provided details" in {
        val requestBody = Json.stringify(Json.toJson(estateRegRequest)(EstateRegistration.estateRegistrationWriteToDes))

        stubForPost(server, "/estates/registration", requestBody, FORBIDDEN, Json.stringify(jsonResponseAlreadyRegistered))
        val futureResult = connector.registerEstate(estateRegRequest)

        whenReady(futureResult) {
          result => result mustBe AlreadyRegisteredResponse
        }
      }
    }

    "return NoMatch response" when {
      "estates is already registered with provided details" in {
        val requestBody = Json.stringify(Json.toJson(estateRegRequest)(EstateRegistration.estateRegistrationWriteToDes))

        stubForPost(server, "/estates/registration", requestBody, FORBIDDEN, Json.stringify(jsonResponse403NoMatch))
        val futureResult = connector.registerEstate(estateRegRequest)

        whenReady(futureResult) {
          result => result mustBe NoMatchResponse
        }
      }
    }

    "return ServiceUnavailable response" when {
      "DES dependent service is not responding" in {
        val requestBody = Json.stringify(Json.toJson(estateRegRequest)(EstateRegistration.estateRegistrationWriteToDes))
        stubForPost(server, "/estates/registration", requestBody, SERVICE_UNAVAILABLE, Json.stringify(jsonResponse503))
        val futureResult = connector.registerEstate(estateRegRequest)

        whenReady(futureResult) {
          result => result mustBe RegistrationFailureResponse(SERVICE_UNAVAILABLE)
        }
      }
    }

    "return InternalServerError response" when {
      "DES is experiencing some problem" in {
        val requestBody = Json.stringify(Json.toJson(estateRegRequest)(EstateRegistration.estateRegistrationWriteToDes))

        stubForPost(server, "/estates/registration", requestBody, INTERNAL_SERVER_ERROR, Json.stringify(jsonResponse500))

        val futureResult = connector.registerEstate(estateRegRequest)

        whenReady(futureResult) {
          result => result mustBe RegistrationFailureResponse(INTERNAL_SERVER_ERROR)
        }
      }
    }

    "return Forbidden response" when {
      "DES is returning 403 without ALREADY REGISTERED code" in {
        val requestBody = Json.stringify(Json.toJson(estateRegRequest)(EstateRegistration.estateRegistrationWriteToDes))

        stubForPost(server, "/estates/registration", requestBody, FORBIDDEN, "{}")
        val futureResult = connector.registerEstate(estateRegRequest)

        whenReady(futureResult) {
          result => result mustBe RegistrationFailureResponse(FORBIDDEN)
        }
      }
    }
  }

  ".getSubscriptionId" should {

    "return subscription Id" when {
      "valid trn has been submitted" in {
        val trn = "XTRN1234567"
        val subscriptionIdEndpointUrl = s"/trusts/trn/$trn/subscription"
        stubForGet(server, subscriptionIdEndpointUrl,  OK, """{"subscriptionId": "987654321"}""")

        val futureResult = connector.getSubscriptionId(trn)

        whenReady(futureResult) {
          result => result mustBe SubscriptionIdResponse("987654321")
        }
      }
    }

    "return BadRequestException" when {
      "invalid trn has been submitted" in {
        val trn = "invalidtrn"
        val subscriptionIdEndpointUrl = s"/trusts/trn/$trn/subscription"
        stubForGet(server, subscriptionIdEndpointUrl,  BAD_REQUEST,Json.stringify(jsonResponse400GetSubscriptionId))

        val futureResult = connector.getSubscriptionId(trn)

        whenReady(futureResult.failed) {
          result => result mustBe BadRequestException
        }
      }
    }

    "return NotFoundException" when {
      "trn submitted has no data in des " in {
        val trn = "notfoundtrn"
        val subscriptionIdEndpointUrl = s"/trusts/trn/$trn/subscription"
        stubForGet(server, subscriptionIdEndpointUrl,  NOT_FOUND ,Json.stringify(jsonResponse404GetSubscriptionId))

        val futureResult = connector.getSubscriptionId(trn)

        whenReady(futureResult.failed) {
          result => result mustBe NotFoundException
        }
      }
    }

    "return ServiceUnavailableException" when {
      "DES dependent service is not responding" in {
        val trn = "XTRN1234567"
        val subscriptionIdEndpointUrl = s"/trusts/trn/$trn/subscription"
        stubForGet(server, subscriptionIdEndpointUrl,  SERVICE_UNAVAILABLE ,Json.stringify(jsonResponse503))

        val futureResult = connector.getSubscriptionId(trn)

        whenReady(futureResult.failed) {
          result => result mustBe an[ServiceNotAvailableException]
        }
      }
    }

    "return InternalServerErrorException" when {
      "DES is experiencing some problem" in {
        val trn = "XTRN1234567"
        val subscriptionIdEndpointUrl = s"/trusts/trn/$trn/subscription"
        stubForGet(server, subscriptionIdEndpointUrl,  INTERNAL_SERVER_ERROR ,Json.stringify(jsonResponse500))

        val futureResult = connector.getSubscriptionId(trn)

        whenReady(futureResult.failed) {
          result => result mustBe an[InternalServerErrorException]
        }
      }
    }
  }

  ".getEstateInfo" should {

    "return EstateFoundResponse" when {

      "DES has returned a 200 with estate details" in {
        val utr = "1234567890"
        stubForGet(server, createTrustOrEstateEndpoint(utr), OK, getEstateResponseJson)

        val futureResult = connector.getEstateInfo(utr)

        whenReady(futureResult) { result =>
          Json.toJson(result) mustBe getEstateExpectedResponse
        }
      }

      "DES has returned a 200 and indicated that the submission is still being processed" in {
        val utr = "1234567800"
        stubForGet(server, createTrustOrEstateEndpoint(utr), OK, getTrustOrEstateProcessingResponseJson)

        val futureResult = connector.getEstateInfo(utr)

        whenReady(futureResult) { result =>
          result mustBe GetEstateStatusResponse(ResponseHeader("In Processing", "1"))
        }
      }

      "DES has returned a 200 and indicated that the submission is pending closure" in {
        val utr = "1234567800"
        stubForGet(server, createTrustOrEstateEndpoint(utr), OK, getTrustOrEstatePendingClosureResponseJson)

        val futureResult = connector.getEstateInfo(utr)

        whenReady(futureResult) { result =>
          result mustBe GetEstateStatusResponse(ResponseHeader("Pending Closure", "1"))
        }
      }

      "DES has returned a 200 and indicated that the submission is closed" in {
        val utr = "1234567800"
        stubForGet(server, createTrustOrEstateEndpoint(utr), OK, getTrustOrEstateClosedResponseJson)

        val futureResult = connector.getEstateInfo(utr)

        whenReady(futureResult) { result =>
          result mustBe GetEstateStatusResponse(ResponseHeader("Closed", "1"))
        }
      }

      "DES has returned a 200 and indicated that the submission is suspended" in {
        val utr = "1234567800"
        stubForGet(server, createTrustOrEstateEndpoint(utr), OK, getTrustOrEstateSuspendedResponseJson)

        val futureResult = connector.getEstateInfo(utr)

        whenReady(futureResult) { result =>
          result mustBe GetEstateStatusResponse(ResponseHeader("Suspended", "1"))
        }
      }

      "DES has returned a 200 and indicated that the submission is parked" in {
        val utr = "1234567800"
        stubForGet(server, createTrustOrEstateEndpoint(utr), OK, getTrustOrEstateParkedResponseJson)

        val futureResult = connector.getEstateInfo(utr)

        whenReady(futureResult) { result =>
          result mustBe GetEstateStatusResponse(ResponseHeader("Parked", "1"))
        }
      }

      "DES has returned a 200 and indicated that the submission is obsoleted" in {
        val utr = "1234567800"
        stubForGet(server, createTrustOrEstateEndpoint(utr), OK, getTrustOrEstateObsoletedResponseJson)

        val futureResult = connector.getEstateInfo(utr)

        whenReady(futureResult) { result =>
          result mustBe GetEstateStatusResponse(ResponseHeader("Obsoleted", "1"))
        }
      }
    }

    "return InvalidUTRResponse" when {
      "DES has returned a 400 with the code INVALID_UTR" in {
        val invalidUTR = "123456789"
        stubForGet(server, createTrustOrEstateEndpoint(invalidUTR), BAD_REQUEST,
          Json.stringify(jsonResponse400InvalidUTR))

        val futureResult = connector.getEstateInfo(invalidUTR)

        whenReady(futureResult) { result =>
          result mustBe InvalidUTRResponse
        }
      }
    }

    "return InvalidRegimeResponse" when {
      "DES has returned a 400 with the code INVALID_REGIME" in {
        val utr = "1234567891"
        stubForGet(server, createTrustOrEstateEndpoint(utr), BAD_REQUEST,
          Json.stringify(jsonResponse400InvalidRegime))

        val futureResult = connector.getEstateInfo(utr)

        whenReady(futureResult) { result =>
          result mustBe InvalidRegimeResponse
        }
      }
    }

    "return BadRequestResponse" when {
      "DES has returned a 400 with a code which is not INVALID_UTR OR INVALID_REGIME" in {
        val utr = "1234567891"
        stubForGet(server, createTrustOrEstateEndpoint(utr), BAD_REQUEST,
          Json.stringify(jsonResponse400))

        val futureResult = connector.getEstateInfo(utr)

        whenReady(futureResult) { result =>
          result mustBe BadRequestResponse
        }
      }
    }

    "return ResourceNotFoundResponse" when {
      "DES has returned a 404" in {
        val utr = "1234567892"
        stubForGet(server, createTrustOrEstateEndpoint(utr), NOT_FOUND, "")

        val futureResult = connector.getEstateInfo(utr)

        whenReady(futureResult) { result =>
          result mustBe ResourceNotFoundResponse
        }
      }
    }

    "return NotEnoughDataResponse" when {

      "no response header" in {
        val utr = "6666666666"
        stubForGet(server, createTrustOrEstateEndpoint(utr), OK, Json.obj().toString())

        val futureResult = connector.getEstateInfo(utr)

        whenReady(futureResult) { result =>
          result mustBe NotEnoughDataResponse(Json.obj(), JsError.toJson(JsError("responseHeader not defined on response")))
        }
      }

      "body does not validate as GetEstate" in {
        val utr = "2000000000"
        stubForGet(server, createTrustOrEstateEndpoint(utr), OK, getEstateInvalidResponseJson.toString())

        val futureResult = connector.getEstateInfo(utr)

        whenReady(futureResult) { result =>
          result mustBe NotEnoughDataResponse(getEstateInvalidResponseJson, Json.parse("{\"obj.details.estate.entities.personalRepresentative\":[{\"msg\":[\"error.path.missing\"],\"args\":[]}]}"))
        }
      }
    }

    "return InternalServerErrorResponse" when {
      "DES has returned a 500 with the code SERVER_ERROR" in {
        val utr = "1234567893"
        stubForGet(server, createTrustOrEstateEndpoint(utr), INTERNAL_SERVER_ERROR, "")

        val futureResult = connector.getEstateInfo(utr)

        whenReady(futureResult) { result =>
          result mustBe InternalServerErrorResponse
        }
      }
    }

    "return ServiceUnavailableResponse" when {
      "DES has returned a 503 with the code SERVICE_UNAVAILABLE" in {
        val utr = "1234567894"
        stubForGet(server, createTrustOrEstateEndpoint(utr), SERVICE_UNAVAILABLE, "")

        val futureResult = connector.getEstateInfo(utr)

        whenReady(futureResult) { result =>
          result mustBe ServiceUnavailableResponse
        }
      }
    }
  }

  ".EstateVariation" should {

    val url = "/estates/variation"

    "return a VariationTrnResponse" when {

      "DES has returned a 200 with a trn" in {

        val requestBody = Json.stringify(Json.toJson(estateVariationsRequest))
        stubForPost(server, url, requestBody, OK, """{"tvn": "XXTVN1234567890"}""")

        val futureResult = connector.estateVariation(estateVariationsRequest)

        whenReady(futureResult) { result =>
          result mustBe a[VariationSuccessResponse]
          inside(result){ case VariationSuccessResponse(tvn)  => tvn must fullyMatch regex """^[a-zA-Z0-9]{15}$""".r }
        }
      }
    }

    "payload sent to DES is invalid" in {

      val variation = estateVariationsRequest

      val requestBody = Json.stringify(Json.toJson(variation))
      stubForPost(server, url, requestBody, BAD_REQUEST, Json.stringify(jsonResponse400))

      val futureResult = connector.estateVariation(variation)

      whenReady(futureResult) {
        result =>
          result mustBe VariationFailureResponse(InvalidRequestErrorResponse)
      }

    }

    "return DuplicateSubmission response" when {
      "trusts two requests are submitted with the same Correlation ID" in {

        val requestBody = Json.stringify(Json.toJson(estateVariationsRequest))

        stubForPost(server, url, requestBody, CONFLICT, Json.stringify(jsonResponse409DuplicateCorrelation))
        val futureResult = connector.estateVariation(estateVariationsRequest)

        whenReady(futureResult) {
          result =>
            result mustBe VariationFailureResponse(DuplicateSubmissionErrorResponse)
        }
      }
    }

    "return InvalidCorrelationId response" when {
      "trusts provides an invalid Correlation ID" in {
        val requestBody = Json.stringify(Json.toJson(estateVariationsRequest))

        stubForPost(server, url, requestBody, BAD_REQUEST, Json.stringify(jsonResponse400CorrelationId))
        val futureResult = connector.estateVariation(estateVariationsRequest)

        whenReady(futureResult) {
          result =>
            result mustBe VariationFailureResponse(InvalidCorrelationIdErrorResponse)
        }
      }
    }

    "return ServiceUnavailable response" when {
      "DES dependent service is not responding" in {
        val requestBody = Json.stringify(Json.toJson(estateVariationsRequest))

        stubForPost(server, url, requestBody, SERVICE_UNAVAILABLE, Json.stringify(jsonResponse503))

        val futureResult = connector.estateVariation(estateVariationsRequest)

        whenReady(futureResult) {
          result =>
            result mustBe VariationFailureResponse(ServiceUnavailableErrorResponse)
        }
      }
    }

    "return InternalServerError response" when {
      "DES is experiencing some problem" in {
        val requestBody = Json.stringify(Json.toJson(estateVariationsRequest))

        stubForPost(server, url, requestBody, INTERNAL_SERVER_ERROR, Json.stringify(jsonResponse500))

        val futureResult = connector.estateVariation(estateVariationsRequest)

        whenReady(futureResult) {
          result =>
            result mustBe VariationFailureResponse(InternalServerErrorErrorResponse)
        }
      }
    }
  }
}
