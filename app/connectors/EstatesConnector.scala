/*
 * Copyright 2024 HM Revenue & Customs
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

import config.AppConfig
import models._
import models.getEstate.GetEstateResponse
import models.variation.VariationResponse
import play.api.Logging
import play.api.http.HeaderNames
import play.api.libs.json._
import services.Estates5MLDService
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import utils.Constants._

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EstatesConnector @Inject()(http: HttpClientV2, config: AppConfig, estates5MLDService: Estates5MLDService)(implicit ec: ExecutionContext) extends Logging {

  private lazy val estatesServiceUrl: String = s"${config.registrationBaseUrl}/estates"

  private lazy val matchEstatesEndpoint: String = s"$estatesServiceUrl/match"

  private lazy val estateRegistrationEndpoint: String = s"$estatesServiceUrl/registration"

  // When reading estates from DES, it's the same endpoint as for trusts.
  // So this must remain "trusts" even though we're reading an estate.
  private lazy val getEstateUrl: String = s"${config.getEstateBaseUrl}/trusts"

  private def create5MLDEstateEndpointForUtr(utr: String): String = s"$getEstateUrl/registration/UTR/$utr"

  private lazy val estateVariationsEndpoint: String = s"${config.varyEstateBaseUrl}/estates/variation"

  private val ENVIRONMENT_HEADER = "Environment"
  private val CORRELATION_HEADER = "CorrelationId"

  private def headers(correlationId: String,
                      token: String,
                      environment: String): Seq[(String, String)] = Seq(
    HeaderNames.AUTHORIZATION -> s"Bearer $token",
    CONTENT_TYPE -> CONTENT_TYPE_JSON,
    ENVIRONMENT_HEADER -> environment,
    CORRELATION_HEADER -> correlationId
  )

  def checkExistingEstate(existingEstateCheckRequest: ExistingCheckRequest)
  : Future[ExistingCheckResponse] = {
    val correlationId = UUID.randomUUID().toString

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = headers(
      correlationId = correlationId,
      token = config.registrationToken,
      environment = config.registrationEnvironment
    ))

    logger.info(s"[checkExistingEstate] matching estate for correlationId: $correlationId")
    val url = matchEstatesEndpoint
    http
      .post(url"$url")
      .withBody(Json.toJson(existingEstateCheckRequest))
      .execute[ExistingCheckResponse]
  }

  def registerEstate(registration: EstateRegistration): Future[RegistrationResponse] = {
    val correlationId = UUID.randomUUID().toString

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = headers(
      correlationId = correlationId,
      token = config.registrationToken,
      environment = config.registrationEnvironment
    ))

    logger.info(s"[registerEstate] registering estate for correlationId: $correlationId")

    val url = (estateRegistrationEndpoint)
    http
      .post(url"$url")
      .withBody(Json.toJson(registration)(EstateRegistration.estateRegistrationWriteToDes))
      .execute[RegistrationResponse]
  }

  def getEstateInfo(utr: String): Future[GetEstateResponse] = {
    val correlationId = UUID.randomUUID().toString

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = headers(
      correlationId = correlationId,
      token = config.playbackToken,
      environment = config.playbackEnvironment
    ))

    logger.info(s"[getEstateInfo][UTR: $utr] getting playback for estate for correlationId: $correlationId")

    val url = (create5MLDEstateEndpointForUtr(utr))
    http
      .get(url"$url")
      .execute(GetEstateResponse.httpReads(utr), ec)
  }

    def estateVariation(estateVariations: JsValue): Future[VariationResponse] = {
      val correlationId = UUID.randomUUID().toString

      implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = headers(
        correlationId = correlationId,
        token = config.variationToken,
        environment = config.variationEnvironment
      ))

      logger.info(s"[estateVariation] submitting estate variation for correlationId: $correlationId")
      val url = (estateVariationsEndpoint)

      http
        .post(url"$url")
        .withBody(Json.toJson(estateVariations))
        .execute[VariationResponse]
    }
}