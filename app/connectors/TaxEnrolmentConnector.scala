/*
 * Copyright 2022 HM Revenue & Customs
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

import com.google.inject.ImplementedBy
import config.AppConfig
import models.{TaxEnrolmentSubscriberResponse, TaxEnrolmentSubscription}
import play.api.libs.json.{JsValue, Json, Writes}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import utils.Constants._

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TaxEnrolmentConnectorImpl @Inject()(http: HttpClient, config: AppConfig) extends TaxEnrolmentConnector {

  private def headers =
    Seq(
      CONTENT_TYPE -> CONTENT_TYPE_JSON
    )

  override def enrolSubscriber(subscriptionId: String)(implicit hc: HeaderCarrier) :  Future[TaxEnrolmentSubscriberResponse] = {
    val taxEnrolmentsEndpoint = s"${config.taxEnrolmentsBaseUrl}/tax-enrolments/subscriptions/$subscriptionId/subscriber"
    val taxEnolmentHeaders = hc.withExtraHeaders(headers: _*)

    val taxEnrolmentSubscriptionRequest = TaxEnrolmentSubscription(
      serviceName = "HMRC-TERS-ORG",
      callback = config.taxEnrolmentsPayloadBodyCallback,
      etmpId = subscriptionId)

    val response = http.PUT[JsValue, TaxEnrolmentSubscriberResponse](taxEnrolmentsEndpoint, Json.toJson(taxEnrolmentSubscriptionRequest))
    (Writes.jsValueWrites, TaxEnrolmentSubscriberResponse.httpReads, taxEnolmentHeaders.headers _, global)
    response
  }

}

@ImplementedBy(classOf[TaxEnrolmentConnectorImpl])
trait TaxEnrolmentConnector {
  def enrolSubscriber(subscriptionId: String)(implicit hc: HeaderCarrier):  Future[TaxEnrolmentSubscriberResponse]
}
