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
import models.getEstate.GetEstateResponse
import models.variation.VariationResponse
import models._
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json, OFormat}
import services.Estates5MLDService
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, StringContextOps}

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HipEstatesConnector @Inject() (http: HttpClientV2, config: AppConfig, estates5MLDService: Estates5MLDService)(
  implicit ec: ExecutionContext
) extends Logging with EstatesConnector {

  private lazy val estatesServiceUrl: String = s"${config.hipRegistrationBaseUrl}/etmp/RESTAdapter/trustsandestates"

  private lazy val matchEstatesEndpoint: String = s"$estatesServiceUrl/match"

  private lazy val estateRegistrationEndpoint: String = s"$estatesServiceUrl/registration"

  // When reading estates from DES, it's the same endpoint as for trusts.
  // So this must remain "trusts" even though we're reading an estate.
  private lazy val getEstateUrl: String = s"${config.getEstateBaseUrl}/trusts"

  private def create5MLDEstateEndpointForUtr(utr: String): String = s"$getEstateUrl/registration/UTR/$utr"

  private lazy val estateVariationsEndpoint: String =
    s"${config.varyEstateBaseUrl}/etmp/RESTAdapter/trustsandestates/variation"

  protected def hipHeaders: Seq[(String, String)] =
    Seq(
      "correlationid"         -> UUID.randomUUID().toString,
      "X-Originating-System"  -> "TRS",
      "X-Receipt-Date"        -> DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
      "X-Transmitting-System" -> "HIP",
      "Authorization"         -> s"Basic ${config.hipAuthorizationToken}"
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

  override def registerEstate(registration: EstateRegistration): Future[RegistrationResponse] = ???

  override def getEstateInfo(utr: String): Future[GetEstateResponse] = ???

  override def estateVariation(estateVariations: JsValue): Future[VariationResponse] = ???

}
