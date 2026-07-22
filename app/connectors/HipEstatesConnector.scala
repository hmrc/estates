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

import cats.implicits.catsSyntaxEq
import config.AppConfig
import models.ExistingCheckResponse.{
  AlreadyRegistered, BadRequest, Matched, NotMatched, ServerError, ServiceUnavailable
}
import models._
import models.getEstate._
import models.variation.VariationResponse.failure
import models.variation.{HipSuccessVariationTrnResponse, VariationResponse}
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, StringContextOps}
import utils.Constants._
import utils.ErrorResponses.{
  DuplicateSubmissionErrorResponse, InternalServerErrorErrorResponse, InvalidRequestErrorResponse,
  ServiceUnavailableErrorResponse
}

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HipEstatesConnector @Inject() (http: HttpClientV2, config: AppConfig)(implicit ec: ExecutionContext)
    extends Logging with EstatesConnector {

  private lazy val estatesServiceUrl: String = s"${config.hipRegistrationBaseUrl}/etmp/RESTAdapter/trustsandestates"

  private lazy val matchEstatesEndpoint: String = s"$estatesServiceUrl/match"

  private lazy val estateRegistrationEndpoint: String = s"$estatesServiceUrl/registration"

  private lazy val getEstateUrl: String = s"${config.hipGetEstateBaseUrl}/etmp/RESTAdapter/trustsandestates"

  private def create5MLDEstateEndpointForUtr(utr: String): String = s"$getEstateUrl/registration/UTR/$utr"

  private lazy val estateVariationsEndpoint: String =
    s"${config.hipVaryEstateBaseUrl}/etmp/RESTAdapter/trustsandestates/registration"

  protected def hipHeaders: Seq[(String, String)] =
    Seq(
      "correlationid"         -> UUID.randomUUID().toString,
      "X-Originating-System"  -> "TRS",
      "X-Receipt-Date"        -> DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
      "X-Transmitting-System" -> "HIP",
      "Authorization"         -> s"Basic ${config.hipAuthorizationToken}",
      CONTENT_TYPE            -> CONTENT_TYPE_JSON
    )

  override def checkExistingEstate(existingEstateCheckRequest: ExistingCheckRequest): Future[ExistingCheckResponse] = {

    implicit val hc: HeaderCarrier                                   = HeaderCarrier(extraHeaders = hipHeaders)
    implicit val hipCustomErrResponse: OFormat[HipCustomErrResponse] = HipCustomErrResponse.formats

    val httpReads: HttpReads[ExistingCheckResponse] =
      new HttpReads[ExistingCheckResponse] {
        override def read(method: String, url: String, response: HttpResponse): ExistingCheckResponse =
          response.status match {
            case CREATED                                            =>
              Matched
            case UNPROCESSABLE_ENTITY                               =>
              val code = response.json.as[HipCustomErrResponse].error.errorId
              if (code === "001")
                NotMatched
              else if (code === "002")
                AlreadyRegistered
              else if (code === "999")
                ServerError
              else
                BadRequest
            case BAD_REQUEST | NOT_FOUND | UNAUTHORIZED | FORBIDDEN =>
              BadRequest
            case INTERNAL_SERVER_ERROR                              =>
              ServerError
            case _                                                  =>
              ServiceUnavailable
          }
      }

    logger.info(
      s"[checkExistingEstate] matching estate for correlationid: ${hipHeaders.toMap.getOrElse("correlationid", "NOT FOUND")}"
    )
    val url = matchEstatesEndpoint
    http
      .post(url"$url")
      .withBody(Json.toJson(existingEstateCheckRequest))
      .execute[ExistingCheckResponse](using httpReads, ec)
  }

  override def registerEstate(registration: EstateRegistration): Future[RegistrationResponse] = {

    implicit val hc: HeaderCarrier                                   = HeaderCarrier(extraHeaders = hipHeaders)
    implicit val hipCustomErrResponse: OFormat[HipCustomErrResponse] = HipCustomErrResponse.formats

    val httpReads: HttpReads[RegistrationResponse] =
      (_: String, _: String, response: HttpResponse) =>
        response.status match {
          case CREATED                                =>
            response.json.as[HipSuccessRegistrationTrnResponse].success
          case UNPROCESSABLE_ENTITY                   =>
            val code = response.json.as[HipCustomErrResponse].error.errorId
            if (code === "001") {
              NoMatchResponse
            } else if (code === "002") {
              AlreadyRegisteredResponse
            } else if (code === "999") {
              RegistrationFailureResponse(INTERNAL_SERVER_ERROR)
            } else {
              RegistrationFailureResponse(UNPROCESSABLE_ENTITY)
            }
          case BAD_REQUEST | NOT_FOUND | UNAUTHORIZED =>
            RegistrationFailureResponse(BAD_REQUEST)
          case INTERNAL_SERVER_ERROR | FORBIDDEN      =>
            RegistrationFailureResponse(INTERNAL_SERVER_ERROR)
          case _                                      =>
            RegistrationFailureResponse(SERVICE_UNAVAILABLE)
        }

    logger.info(
      s"[registerEstate] registering estate for correlationid: ${hipHeaders.toMap.getOrElse("correlationid", "NOT FOUND")}"
    )

    http
      .post(url"$estateRegistrationEndpoint")
      .withBody(Json.toJson(registration)(EstateRegistration.estateRegistrationWriteToDes))
      .execute[RegistrationResponse](using httpReads, ec)
  }

  override def getEstateInfo(utr: String): Future[GetEstateResponse] = {

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = hipHeaders)

    logger.info(
      s"[getEstateInfo][UTR: $utr] getting playback for estate for correlationId: " +
        s"${hipHeaders.toMap.getOrElse("correlationid", "not found")}"
    )

    def httpReads(utr: String): HttpReads[GetEstateResponse] = (_: String, _: String, response: HttpResponse) =>
      response.status match {
        case OK                                                                                 =>
          println(s"response: ${response.json}")
          parseOkResponse(response, utr)
        case BAD_REQUEST                                                                        =>
          logger.warn(
            s"[UTR: $utr]" +
              s" bad request returned from des: ${response.body}"
          )
          BadRequestResponse
        case UNPROCESSABLE_ENTITY if (response.json \ "error" \ "errorId").as[String] === "000" =>
          notFoundResponse(UNPROCESSABLE_ENTITY, utr)
        case NOT_FOUND                                                                          =>
          notFoundResponse(NOT_FOUND, utr)
        case SERVICE_UNAVAILABLE                                                                =>
          logger.warn(
            s"[UTR: $utr]" +
              s" service is unavailable, unable to get trust"
          )
          ServiceUnavailableResponse
        case status                                                                             =>
          logger.error(
            s"[UTR: $utr]" +
              s" error occurred when getting trust, status: $status"
          )
          InternalServerErrorResponse
      }

    def notFoundResponse(status: Int, utr: String) = {
      logger.info(
        s"[UTR: $utr]" +
          s" trust not found in ETMP for given identifier" +
          s"with response code from HIP: $status"
      )
      ResourceNotFoundResponse
    }

    def parseOkResponse(response: HttpResponse, utr: String): GetEstateResponse =
      response.json.validate[HipSuccessGetEstateResponseWrapper] match {
        case JsSuccess(estateFound, _) => estateFound.success
        case JsError(errors)           =>
          logger.error(s"[UTR: $utr] Cannot parse as EstateFoundResponse due to ${JsError.toJson(errors)}")
          NotEnoughDataResponse(response.json, JsError.toJson(errors))
      }

    val url = create5MLDEstateEndpointForUtr(utr)

    http
      .get(url"$url")
      .execute(httpReads(utr), ec)
  }

  override def estateVariation(estateVariations: JsValue): Future[VariationResponse] = {

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = hipHeaders)

    logger.info(
      s"[estateVariation] submitting estate variation for correlationid: ${hipHeaders.toMap.getOrElse("correlationid", "NOT FOUND")}"
    )

    val url                                     = estateVariationsEndpoint
    val httpReads: HttpReads[VariationResponse] = new HttpReads[VariationResponse] {
      override def read(method: String, url: String, response: HttpResponse): VariationResponse =
        response.status match {
          case OK                    =>
            val hip = response.json.as[HipSuccessVariationTrnResponse]
            hip.success
          case BAD_REQUEST           =>
            logger.error(s"[VariationResponse][httpReads] Bad Request response from hip")
            failure(InvalidRequestErrorResponse)
          case UNPROCESSABLE_ENTITY  =>
            val code = response.json.as[HipCustomErrResponse].error.errorId
            if (code === "004") {
              logger.info("[VariationResponse] Duplicate submission response from HIP.")
              failure(DuplicateSubmissionErrorResponse)
            } else if (code === "003") {
              logger.info("[VariationResponse] Request could not be processed response from hip")
              failure(InvalidRequestErrorResponse)
            } else if (code === "999") {
              logger.error("[RegistrationResponse] Technical error response from HIP.")
              failure(InternalServerErrorErrorResponse)
            } else
              failure(InvalidRequestErrorResponse)
          case INTERNAL_SERVER_ERROR =>
            logger.error(s"[VariationResponse][httpReads] Internal server error response from hip")
            failure(InternalServerErrorErrorResponse)
          case SERVICE_UNAVAILABLE   =>
            failure(ServiceUnavailableErrorResponse)
          case status                =>
            logger.error(s"[VariationResponse][httpReads] $status response from hip.")
            failure(ErrorResponse(status.toString, s"Error response from DES: $status"))
        }
    }

    http
      .put(url"$url")
      .withBody(Json.toJson(estateVariations))
      .execute[VariationResponse](using httpReads, ec)
  }

}
