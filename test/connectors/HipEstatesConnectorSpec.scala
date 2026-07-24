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

package connectors

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import models.ExistingCheckResponse._
import models.getEstate._
import models.variation.{VariationFailureResponse, VariationSuccessResponse}
import models._
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsError, Json}
import play.api.test.Helpers.CONTENT_TYPE
import utils.ErrorResponses._
import utils.JsonRequests

class HipEstatesConnectorSpec extends BaseConnectorSpec with JsonRequests {

  lazy val connector: HipEstatesConnector = injector.instanceOf[HipEstatesConnector]

  lazy val request: ExistingCheckRequest = ExistingCheckRequest("trust name", postcode = Some("NE65TA"), "1234567890")

  override def applicationBuilder(): GuiceApplicationBuilder =
    super
      .applicationBuilder()
      .configure(
        Seq(
          "microservice.services.hip.registration.port" -> server.port(),
          "microservice.services.hip.variation.port"    -> server.port(),
          "microservice.services.hip.playback.port"     -> server.port()
        ): _*
      )

  def create5MLDTrustOrEstateEndpoint(utr: String) = s"/etmp/RESTAdapter/trustsandestates/registration/UTR/$utr"

  override def stubForPost(
    server: WireMockServer,
    url: String,
    requestBody: String,
    returnStatus: Int,
    responseBody: String,
    delayResponse: Int = 0
  ) =

    server.stubFor(
      post(urlEqualTo(url))
        .withHeader(CONTENT_TYPE, containing("application/json"))
        .withRequestBody(equalTo(requestBody))
        .willReturn(
          aResponse()
            .withStatus(returnStatus)
            .withBody(responseBody)
            .withFixedDelay(delayResponse)
        )
    )

  ".EstateVariation" should {

    val url = "/etmp/RESTAdapter/trustsandestates/registration"

    "return a VariationTrnResponse" when {

      "Hip has returned a 200 with a trn" in {

        val requestBody = Json.stringify(Json.toJson(estateVariationsRequest))
        stubForPutWithResponseBody(server, url, requestBody, OK, """{"success": {"tvn": "XXTVN1234567890"}}""")

        val futureResult = connector.estateVariation(estateVariationsRequest)

        whenReady(futureResult) { result =>
          result mustBe a[VariationSuccessResponse]
          inside(result) { case VariationSuccessResponse(tvn) => tvn must fullyMatch regex """^[a-zA-Z0-9]{15}$""".r }
        }
      }
    }

    "return InvalidRequestErrorResponse" when {
      "payload sent to hip is invalid" in {
        val variation   = estateVariationsRequest
        val requestBody = Json.stringify(Json.toJson(variation))
        stubForPutWithResponseBody(
          server,
          url,
          requestBody,
          BAD_REQUEST,
          s"""
             |{
             | "code": "400",
             | "message": "String",
             | "logID": "00000000000000000000000000000000"
             |}""".stripMargin
        )

        val futureResult = connector.estateVariation(Json.toJson(variation))

        whenReady(futureResult) { result =>
          result mustBe VariationFailureResponse(InvalidRequestErrorResponse)
        }
      }
    }

    "return DuplicateSubmission response" when {
      "trusts two requests are submitted with the same Correlation ID" in {

        val requestBody = Json.stringify(Json.toJson(estateVariationsRequest))

        stubForPutWithResponseBody(
          server,
          url,
          requestBody,
          UNPROCESSABLE_ENTITY,
          s"""
             |{
             |  "error":
             |    {
             |      "errorId": "004",
             |      "processingDate": "2001-12-17T09:30:47.0",
             |      "text": "Duplicate submission acknowledgment reference"
             |    }
             |}
             |""".stripMargin
        )

        val futureResult = connector.estateVariation(estateVariationsRequest)

        whenReady(futureResult) { result =>
          result mustBe VariationFailureResponse(DuplicateSubmissionErrorResponse)
        }
      }
    }

    "return ServiceUnavailable response" when {
      "HIP dependent service is not responding" in {
        val requestBody = Json.stringify(Json.toJson(estateVariationsRequest))

        stubForPut(
          server,
          url,
          SERVICE_UNAVAILABLE
        )

        val futureResult = connector.estateVariation(estateVariationsRequest)

        whenReady(futureResult) { result =>
          result mustBe VariationFailureResponse(ServiceUnavailableErrorResponse)
        }
      }
    }

    "return InternalServerErrorErrorResponse response" when {
      "HIP returns 500" in {
        val requestBody = Json.stringify(Json.toJson(estateVariationsRequest))

        stubForPut(
          server,
          url,
          INTERNAL_SERVER_ERROR
        )

        val futureResult = connector.estateVariation(estateVariationsRequest)

        whenReady(futureResult) { result =>
          result mustBe VariationFailureResponse(InternalServerErrorErrorResponse)
        }
      }
    }

    "return InternalServerError response" when {
      "HIP is experiencing some problem" in {
        val requestBody = Json.stringify(Json.toJson(estateVariationsRequest))

        stubForPutWithResponseBody(
          server,
          url,
          requestBody,
          UNPROCESSABLE_ENTITY,
          s"""
             |{
             |  "error":
             |    {
             |      "errorId": "999",
             |      "processingDate": "2001-12-17T09:30:47.0",
             |      "text": "Technical System Error"
             |    }
             |}
             |""".stripMargin
        )

        val futureResult = connector.estateVariation(estateVariationsRequest)

        whenReady(futureResult) { result =>
          result mustBe VariationFailureResponse(InternalServerErrorErrorResponse)
        }
      }
    }

    "return InvalidRequestErrorResponse response" when {
      "HIP is experiencing an unknown problem" in {
        val requestBody = Json.stringify(Json.toJson(estateVariationsRequest))

        stubForPutWithResponseBody(
          server,
          url,
          requestBody,
          UNPROCESSABLE_ENTITY,
          s"""
             |{
             |  "error":
             |    {
             |      "errorId": "000",
             |      "processingDate": "2001-12-17T09:30:47.0",
             |      "text": "Unknown"
             |    }
             |}
             |""".stripMargin
        )

        val futureResult = connector.estateVariation(estateVariationsRequest)

        whenReady(futureResult) { result =>
          result mustBe VariationFailureResponse(InvalidRequestErrorResponse)
        }
      }
    }

    "return ErrorResponse  " when {
      "HIP sends an unknown status " in {
        val requestBody = Json.stringify(Json.toJson(estateVariationsRequest))

        stubForPutWithResponseBody(
          server,
          url,
          requestBody,
          IM_A_TEAPOT,
          "foo"
        )

        val futureResult = connector.estateVariation(Json.toJson(estateVariationsRequest))

        whenReady(futureResult) { result =>
          result mustBe VariationFailureResponse(
            ErrorResponse(IM_A_TEAPOT.toString, s"Error response from DES: $IM_A_TEAPOT")
          )
        }
      }
    }

  }

  ".checkExistingEstate" should {

    "return Matched" when {
      "estate data match with existing estate" in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/match",
          requestBody,
          CREATED,
          """{ "success": {"tvn": "XXTVN1234567890"}}"""
        )

        val futureResult = connector.checkExistingEstate(request)

        whenReady(futureResult) { result =>
          result mustBe Matched
        }
      }
    }

    "return NotMatched" when {
      "estate data does not match with existing estate" in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/match",
          requestBody,
          UNPROCESSABLE_ENTITY,
          """{
            |  "error": {
            |    "processingDate": "2001-12-17T09:30:47.0",
            |    "errorId": "001",
            |    "text": "FAIL – NO MATCH"
            |  }
            |}""".stripMargin
        )

        val futureResult = connector.checkExistingEstate(request)

        whenReady(futureResult) { result =>
          result mustBe NotMatched
        }
      }
    }

    "return BadRequest" when {
      "payload sent is not valid" in {
        val wrongPayloadRequest = request.copy(utr = "NUMBER1234")
        val requestBody         = Json.stringify(Json.toJson(wrongPayloadRequest))

        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/match",
          requestBody,
          BAD_REQUEST,
          """{
            |  "error": {
            |    "code": "400",
            |    "message": "String",
            |    "logID": "00000000000000000000000000000000"
            |  }
            |}""".stripMargin
        )

        val futureResult = connector.checkExistingEstate(wrongPayloadRequest)

        whenReady(futureResult) { result =>
          result mustBe BadRequest
        }
      }
    }

    "return BadRequest" when {
      "for all other 422 response status" in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/match",
          requestBody,
          UNPROCESSABLE_ENTITY,
          """{
            |  "error": {
            |    "processingDate": "2001-12-17T09:30:47.0",
            |    "errorId": "004",
            |    "text": "Duplicate submission acknowledgment reference"
            |  }
            |}""".stripMargin
        )

        val futureResult = connector.checkExistingEstate(request)

        whenReady(futureResult) { result =>
          result mustBe BadRequest
        }
      }
    }

    "return AlreadyRegistered" when {
      "estate is already registered with provided details" in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/match",
          requestBody,
          UNPROCESSABLE_ENTITY,
          """{
            |  "error": {
            |    "processingDate": "2001-12-17T09:30:47.0",
            |    "errorId": "002",
            |    "text": "FAIL – ALREADY REGISTERED"
            |  }
            |}""".stripMargin
        )

        val futureResult = connector.checkExistingEstate(request)

        whenReady(futureResult) { result =>
          result mustBe AlreadyRegistered
        }
      }
    }

    "return ServerError" when {
      "422 999 response from hip" in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/match",
          requestBody,
          UNPROCESSABLE_ENTITY,
          """{
            |  "error": {
            |    "processingDate": "2001-12-17T09:30:47.0",
            |    "errorId": "999",
            |    "text": "Technical System Error"
            |  }
            |}""".stripMargin
        )

        val futureResult = connector.checkExistingEstate(request)

        whenReady(futureResult) { result =>
          result mustBe ServerError
        }
      }
    }

    "return BadRequest" when {
      "for 401 response status" in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/match",
          requestBody,
          BAD_REQUEST,
          """{
            |  "error": {
            |    "code": "401",
            |    "message": "String",
            |    "logID": "00000000000000000000000000000000"
            |  }
            |}""".stripMargin
        )

        val futureResult = connector.checkExistingEstate(request)

        whenReady(futureResult) { result =>
          result mustBe BadRequest
        }
      }
    }

    "return BadRequest" when {
      "for 403 response status" in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/match",
          requestBody,
          BAD_REQUEST,
          """{
            |  "error": {
            |    "code": "403",
            |    "message": "String",
            |    "logID": "00000000000000000000000000000000"
            |  }
            |}""".stripMargin
        )

        val futureResult = connector.checkExistingEstate(request)

        whenReady(futureResult) { result =>
          result mustBe BadRequest
        }
      }
    }

    "return BadRequest" when {
      "for 404 response status" in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/match",
          requestBody,
          BAD_REQUEST,
          """{
            |  "error": {
            |    "code": "404",
            |    "message": "String",
            |    "logID": "00000000000000000000000000000000"
            |  }
            |}""".stripMargin
        )

        val futureResult = connector.checkExistingEstate(request)

        whenReady(futureResult) { result =>
          result mustBe BadRequest
        }
      }
    }

    "return ServiceUnavailable" when {
      "for 503 response status" in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/match",
          requestBody,
          SERVICE_UNAVAILABLE,
          """{
                |  "error": {
                |    "code": "503",
                |    "message": "String",
                |    "logID": "00000000000000000000000000000000"
                |  }
                |}""".stripMargin
        )

        val futureResult = connector.checkExistingEstate(request)

        whenReady(futureResult) { result =>
          result mustBe ServiceUnavailable
        }
      }
    }

    "return ServerError" when {
      "for 500 response status" in {
        val requestBody = Json.stringify(Json.toJson(request))

        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/match",
          requestBody,
          INTERNAL_SERVER_ERROR,
          """{
            |  "error": {
            |    "code": "500",
            |    "message": "String",
            |    "logID": "00000000000000000000000000000000"
            |  }
            |}""".stripMargin
        )

        val futureResult = connector.checkExistingEstate(request)

        whenReady(futureResult) { result =>
          result mustBe ServerError
        }
      }
    }

  }

  ".getEstateInfo" should {

    "identifier is UTR" must {
      "return EstateFoundResponse" when {

        "HIP has returned a 200 with estate details" in {
          val utr = "1234567890"
          stubForGet(server, create5MLDTrustOrEstateEndpoint(utr), OK, hipGet5MLDEstateResponseJson)

          val futureResult = connector.getEstateInfo(utr)

          whenReady(futureResult) { result =>
            Json.toJson(result) mustBe get5MLDEstateExpectedResponse
            result
          }
        }

        "HIP has returned a 200 and indicated that the submission is still being processed" in {
          val utr = "1234567800"
          stubForGet(server, create5MLDTrustOrEstateEndpoint(utr), OK, hipGetTrustOrEstateProcessingResponseJson)

          val futureResult = connector.getEstateInfo(utr)

          whenReady(futureResult) { result =>
            result mustBe GetEstateStatusResponse(ResponseHeader("In Processing", "1"))
          }
        }

        "HIP has returned a 200 and indicated that the submission is pending closure" in {
          val utr = "1234567800"

          stubForGet(server, create5MLDTrustOrEstateEndpoint(utr), OK, hipGetTrustOrEstatePendingClosureResponseJson)

          val futureResult = connector.getEstateInfo(utr)

          whenReady(futureResult) { result =>
            result mustBe GetEstateStatusResponse(ResponseHeader("Pending Closure", "1"))
          }
        }

        "HIP has returned a 200 and indicated that the submission is closed" in {
          val utr = "1234567800"
          stubForGet(server, create5MLDTrustOrEstateEndpoint(utr), OK, hipGetTrustOrEstateClosedResponseJson)

          val futureResult = connector.getEstateInfo(utr)

          whenReady(futureResult) { result =>
            result mustBe GetEstateStatusResponse(ResponseHeader("Closed", "1"))
          }
        }

        "HIP has returned a 200 and indicated that the submission is suspended" in {
          val utr = "1234567800"
          stubForGet(server, create5MLDTrustOrEstateEndpoint(utr), OK, hipGetTrustOrEstateSuspendedResponseJson)

          val futureResult = connector.getEstateInfo(utr)

          whenReady(futureResult) { result =>
            result mustBe GetEstateStatusResponse(ResponseHeader("Suspended", "1"))
          }
        }

        "HIP has returned a 200 and indicated that the submission is parked" in {
          val utr = "1234567800"

          stubForGet(server, create5MLDTrustOrEstateEndpoint(utr), OK, hipGetTrustOrEstateParkedResponseJson)

          val futureResult = connector.getEstateInfo(utr)

          whenReady(futureResult) { result =>
            result mustBe GetEstateStatusResponse(ResponseHeader("Parked", "1"))
          }
        }

        "HIP has returned a 200 and indicated that the submission is obsoleted" in {
          val utr = "1234567800"

          stubForGet(server, create5MLDTrustOrEstateEndpoint(utr), OK, hipGetTrustOrEstateObsoletedResponseJson)

          val futureResult = connector.getEstateInfo(utr)

          whenReady(futureResult) { result =>
            result mustBe GetEstateStatusResponse(ResponseHeader("Obsoleted", "1"))
          }
        }
      }

      "return NotEnoughData" when {
        "no response header" in {
          val utr           = "6666666666"
          val emptyResponse = Json.obj()
          stubForGet(server, create5MLDTrustOrEstateEndpoint(utr), OK, emptyResponse.toString())

          val futureResult = connector.getEstateInfo(utr)

          whenReady(futureResult) { result =>
            result mustBe NotEnoughDataResponse(
              emptyResponse,
              JsError.toJson(JsError("responseHeader not defined on response"))
            )
          }
        }

        "body does not validate as GetEstate" in {
          val utr = "2000000000"

          stubForGet(server, create5MLDTrustOrEstateEndpoint(utr), OK, hipGetEstateInvalidResponseJson.toString())

          val futureResult = connector.getEstateInfo(utr)

          whenReady(futureResult) { result =>
            result mustBe NotEnoughDataResponse(
              hipGetEstateInvalidResponseJson,
              Json.parse(
                "{\"obj.details.estate.entities.personalRepresentative\":[{\"msg\":[\"error.path.missing\"],\"args\":[]}]}"
              )
            )
          }
        }
      }

      "return BadRequestResponse" when {

        "hip has returned a 400" in {
          val utr = "1234567891"
          stubForGet(server, create5MLDTrustOrEstateEndpoint(utr), BAD_REQUEST, Json.stringify(jsonResponse4005mld))

          val futureResult = connector.getEstateInfo(utr)

          whenReady(futureResult) { result =>
            result mustBe BadRequestResponse
          }
        }
      }

      "return ResourceNotFoundResponse" when {

        "hip has returned a 404" in {
          val utr = "1234567892"
          stubForGet(server, create5MLDTrustOrEstateEndpoint(utr), NOT_FOUND, "")

          val futureResult = connector.getEstateInfo(utr)

          whenReady(futureResult) { result =>
            result mustBe ResourceNotFoundResponse
          }
        }

        "hip has returned a 422 with errorId: 000 and text: UTR or URN is invalid" in {
          val utr = "1234567893"
          stubForGet(
            server,
            create5MLDTrustOrEstateEndpoint(utr),
            UNPROCESSABLE_ENTITY,
            """{
                |  "error": {
                |    "processingDate": "2001-12-17T09:30:47.0",
                |    "errorId": "000",
                |    "text": "UTR or URN is invalid"
                |  }
                |}""".stripMargin
          )

          val futureResult = connector.getEstateInfo(utr)

          whenReady(futureResult) { result =>
            result mustBe ResourceNotFoundResponse
          }
        }
      }

      "return InternalServerErrorResponse" when {

        "hip has returned a 500 with the code SERVER_ERROR" in {
          val utr = "1234567893"
          stubForGet(server, create5MLDTrustOrEstateEndpoint(utr), INTERNAL_SERVER_ERROR, "")

          val futureResult = connector.getEstateInfo(utr)

          whenReady(futureResult) { result =>
            result mustBe InternalServerErrorResponse
          }
        }

        "hip has returned a 422 with errorId: 003 and text: Request could not be processed" in {
          val utr = "1234567003"
          stubForGet(
            server,
            create5MLDTrustOrEstateEndpoint(utr),
            UNPROCESSABLE_ENTITY,
            """{
                |  "error": {
                |    "processingDate": "2001-12-17T09:30:47.0",
                |    "errorId": "003",
                |    "text": "Request could not be processed"
                |  }
                |}""".stripMargin
          )

          val futureResult = connector.getEstateInfo(utr)

          whenReady(futureResult) { result =>
            result mustBe InternalServerErrorResponse
          }
        }
      }
      "return ServiceUnavailableResponse" when {
        "des has returned a 503 with the code SERVICE_UNAVAILABLE" in {

          stubForGet(
            server,
            "/estates-store/features/5mld",
            OK,
            Json.stringify(
              Json.parse(
                """
                  |{
                  | "name": "5mld",
                  | "isEnabled": true
                  |}""".stripMargin
              )
            )
          )

          val utr = "1234567894"
          stubForGet(server, create5MLDTrustOrEstateEndpoint(utr), SERVICE_UNAVAILABLE, "")

          val futureResult = connector.getEstateInfo(utr)

          whenReady(futureResult) { result =>
            result mustBe ServiceUnavailableResponse
          }
        }
      }
    }
  }

  ".registerEstate" should {

    "return TRN" when {
      "valid request to HIP register an estate" in {
        val requestBody = Json.stringify(Json.toJson(estateRegRequest)(EstateRegistration.estateRegistrationWriteToDes))

        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/registration",
          requestBody,
          CREATED,
          """{"success": {"trn": "XTRN1234567"}}"""
        )

        val futureResult = connector.registerEstate(estateRegRequest)

        whenReady(futureResult) { result =>
          result mustBe RegistrationTrnResponse("XTRN1234567")
        }
      }
    }

    "return BadRequest response" when {
      "payload sent to HIP is invalid" in {
        val requestBody = Json.stringify(Json.toJson(estateRegRequest)(EstateRegistration.estateRegistrationWriteToDes))
        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/registration",
          requestBody,
          BAD_REQUEST,
          s"""
               |{
               |  "error": {
               |    "code": "400",
               |    "message": "String",
               |    "logID": "00000000000000000000000000000000"
               |  }
               |}
               |""".stripMargin
        )

        val futureResult = connector.registerEstate(estateRegRequest)

        whenReady(futureResult) { result =>
          result mustBe RegistrationFailureResponse(BAD_REQUEST)
        }
      }
    }

    "return AlreadyRegisteredResponse" when {
      "estate is already registered with provided details" in {
        val requestBody = Json.stringify(Json.toJson(estateRegRequest)(EstateRegistration.estateRegistrationWriteToDes))

        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/registration",
          requestBody,
          UNPROCESSABLE_ENTITY,
          s"""
               |{
               |  "error": {
               |    "errorId": "002",
               |    "processingDate": "2001-12-17T09:30:47.0",
               |    "text": "FAIL – ALREADY REGISTERED"
               |  }
               |}
               |""".stripMargin
        )

        val futureResult = connector.registerEstate(estateRegRequest)

        whenReady(futureResult) { result =>
          result mustBe AlreadyRegisteredResponse
        }
      }
    }

    "return NoMatch response" when {
      "estate does not match HMRC records" in {
        val requestBody = Json.stringify(Json.toJson(estateRegRequest)(EstateRegistration.estateRegistrationWriteToDes))

        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/registration",
          requestBody,
          UNPROCESSABLE_ENTITY,
          s"""
               |{
               |  "error": {
               |    "errorId": "001",
               |    "processingDate": "2001-12-17T09:30:47.0",
               |    "text": "FAIL – NO MATCH"
               |  }
               |}
               |""".stripMargin
        )

        val futureResult = connector.registerEstate(estateRegRequest)

        whenReady(futureResult) { result =>
          result mustBe NoMatchResponse
        }
      }
    }

    "return RegistrationFailureResponse" when {
      "HIP returns 422 999 technical system error" in {
        val requestBody = Json.stringify(Json.toJson(estateRegRequest)(EstateRegistration.estateRegistrationWriteToDes))

        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/registration",
          requestBody,
          UNPROCESSABLE_ENTITY,
          s"""
               |{
               |  "error": {
               |    "errorId": "999",
               |    "processingDate": "2001-12-17T09:30:47.0",
               |    "text": "Technical System Error"
               |  }
               |}
               |""".stripMargin
        )

        val futureResult = connector.registerEstate(estateRegRequest)

        whenReady(futureResult) { result =>
          result mustBe RegistrationFailureResponse(INTERNAL_SERVER_ERROR)
        }
      }
    }

    "return ServiceUnavailable response" when {
      "HIP dependent service is not responding" in {
        val requestBody = Json.stringify(Json.toJson(estateRegRequest)(EstateRegistration.estateRegistrationWriteToDes))

        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/registration",
          requestBody,
          SERVICE_UNAVAILABLE,
          s"""
               |{
               |  "error": {
               |    "code": "503",
               |    "message": "String",
               |    "logID": "00000000000000000000000000000000"
               |  }
               |}
               |""".stripMargin
        )

        val futureResult = connector.registerEstate(estateRegRequest)

        whenReady(futureResult) { result =>
          result mustBe RegistrationFailureResponse(SERVICE_UNAVAILABLE)
        }
      }
    }

    "return InternalServerError response" when {
      "HIP is experiencing some problem" in {
        val requestBody = Json.stringify(Json.toJson(estateRegRequest)(EstateRegistration.estateRegistrationWriteToDes))

        stubForPost(
          server,
          "/etmp/RESTAdapter/trustsandestates/registration",
          requestBody,
          INTERNAL_SERVER_ERROR,
          s"""
               |{
               |  "error": {
               |    "code": "500",
               |    "message": "String",
               |    "logID": "00000000000000000000000000000000"
               |  }
               |}
               |""".stripMargin
        )

        val futureResult = connector.registerEstate(estateRegRequest)

        whenReady(futureResult) { result =>
          result mustBe RegistrationFailureResponse(INTERNAL_SERVER_ERROR)
        }
      }
    }

    "return Forbidden response" when {
      "HIP is returning 403" in {
        val requestBody = Json.stringify(Json.toJson(estateRegRequest)(EstateRegistration.estateRegistrationWriteToDes))

        stubForPost(server, "/etmp/RESTAdapter/trustsandestates/registration", requestBody, FORBIDDEN, "{}")
        val futureResult = connector.registerEstate(estateRegRequest)

        whenReady(futureResult) { result =>
          result mustBe RegistrationFailureResponse(INTERNAL_SERVER_ERROR)
        }
      }
    }

  }

}
