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

package uk.gov.hmrc.estates.connectors

import java.util.UUID

import javax.inject.Inject
import play.api.Logger
import play.api.http.HeaderNames
import play.api.libs.json._
import uk.gov.hmrc.estates.config.{AppConfig, WSHttp}
import uk.gov.hmrc.estates.models.{EstateRegistration, ExistingCheckRequest, ExistingCheckResponse, RegistrationResponse}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.estates.utils.Constants._

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.{ExecutionContext, Future}


class DesConnector @Inject()(http: WSHttp, config: AppConfig) {

  lazy val estatesServiceUrl : String = s"${config.desEstatesUrl}/estates"

  lazy val matchEstatesEndpoint : String = s"$estatesServiceUrl/match"

  lazy val estateRegistrationEndpoint : String = s"$estatesServiceUrl/registration"

  lazy val getTrustOrEstateUrl: String =  s"${config.getTrustOrEstateUrl}/trusts"

  def createGetTrustOrEstateEndpoint(utr: String): String = s"$getTrustOrEstateUrl/registration/$utr"

  lazy val trustVariationsEndpoint : String = s"${config.varyTrustOrEstateUrl}/trusts/variation"
  lazy val estateVariationsEndpoint : String = s"${config.varyTrustOrEstateUrl}/estates/variation"

  val ENVIRONMENT_HEADER = "Environment"
  val CORRELATION_HEADER = "CorrelationId"
  val OLD_CORRELATION_HEADER = "Correlation-Id"

  private def desHeaders(correlationId : String) : Seq[(String, String)] =
    Seq(
      HeaderNames.AUTHORIZATION -> s"Bearer ${config.desToken}",
      CONTENT_TYPE -> CONTENT_TYPE_JSON,
      ENVIRONMENT_HEADER -> config.desEnvironment,
      CORRELATION_HEADER -> correlationId,
      OLD_CORRELATION_HEADER -> correlationId
    )

  def checkExistingEstate(existingEstateCheckRequest: ExistingCheckRequest)
  : Future[ExistingCheckResponse] = {
    val correlationId = UUID.randomUUID().toString

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeaders(correlationId))

    Logger.info(s"[DesConnector] matching estate for correlationId: $correlationId")

    val response = http.POST[JsValue, ExistingCheckResponse](matchEstatesEndpoint, Json.toJson(existingEstateCheckRequest))
    (implicitly[Writes[JsValue]], ExistingCheckResponse.httpReads, implicitly[HeaderCarrier](hc),implicitly[ExecutionContext])

    response
  }

//  def registerEstate(registration: EstateRegistration): Future[RegistrationResponse] = {
//    val correlationId = UUID.randomUUID().toString
//
//    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeaders(correlationId))
//
//    Logger.info(s"[DesConnector] registering estate for correlationId: $correlationId")
//
//    val response = http.POST[JsValue, RegistrationResponse](estateRegistrationEndpoint, Json.toJson(registration))
//    (implicitly[Writes[JsValue]], RegistrationResponse.httpReads, implicitly[HeaderCarrier](hc),implicitly[ExecutionContext])
//    response
//  }
//
//  def getSubscriptionId(trn: String): Future[SubscriptionIdResponse] = {
//
//    val correlationId = UUID.randomUUID().toString
//
//    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeaders(correlationId))
//
//    val subscriptionIdEndpointUrl = s"$trustsServiceUrl/trn/$trn/subscription"
//    Logger.debug(s"[getSubscriptionId] Sending get subscription id request to DES, url=$subscriptionIdEndpointUrl")
//
//    val response = http.GET[SubscriptionIdResponse](subscriptionIdEndpointUrl)
//    (SubscriptionIdResponse.httpReads, implicitly[HeaderCarrier](hc), implicitly[ExecutionContext])
//
//    response
//  }
//
//  def getEstateInfo(utr: String)(implicit hc: HeaderCarrier): Future[GetEstateResponse] = {
//    val correlationId = UUID.randomUUID().toString
//
//    implicit val hc : HeaderCarrier = HeaderCarrier(extraHeaders = desHeaders(correlationId))
//
//    Logger.info(s"[DesConnector] getting playback for estate for correlationId: $correlationId")
//
//    http.GET[GetEstateResponse](createGetTrustOrEstateEndpoint(utr))(GetEstateResponse.httpReads, implicitly[HeaderCarrier](hc), global)
//  }
//
//  def estateVariation(estateVariations: EstateVariation)(implicit hc: HeaderCarrier): Future[VariationResponse] = {
//    val correlationId = UUID.randomUUID().toString
//
//    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = desHeaders(correlationId))
//
//    Logger.info(s"[DesConnector] submitting estate variation for correlationId: $correlationId")
//
//    http.POST[JsValue, VariationResponse](estateVariationsEndpoint, Json.toJson(estateVariations))(
//      implicitly[Writes[JsValue]], VariationResponse.httpReads, implicitly[HeaderCarrier](hc),implicitly[ExecutionContext])
//
//  }
}
