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

import com.google.inject.ImplementedBy
import config.AppConfig
import models.{TaxEnrolmentSubscriberResponse, TaxEnrolmentSubscription}
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import utils.Constants._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TaxEnrolmentConnectorImpl @Inject()(http: HttpClientV2, config: AppConfig)(implicit ec: ExecutionContext) extends TaxEnrolmentConnector {

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

    val url = taxEnrolmentsEndpoint
    val jsonBody = Json.toJson(taxEnrolmentSubscriptionRequest)

    val response = http
      .put(url"$url")
      .withBody(jsonBody)
      .setHeader(taxEnolmentHeaders.headers: _*)
      .execute(Writes.jsValueWrites, TaxEnrolmentSubscriberResponse.httpReads, ec)
      response

//    val response = http.PUT[JsValue, TaxEnrolmentSubscriberResponse](taxEnrolmentsEndpoint, Json.toJson(taxEnrolmentSubscriptionRequest))
//    (Writes.jsValueWrites, TaxEnrolmentSubscriberResponse.httpReads, taxEnolmentHeaders.headers _, ec)
//    response
  }

}

@ImplementedBy(classOf[TaxEnrolmentConnectorImpl])
trait TaxEnrolmentConnector {
  def enrolSubscriber(subscriptionId: String)(implicit hc: HeaderCarrier):  Future[TaxEnrolmentSubscriberResponse]
}
