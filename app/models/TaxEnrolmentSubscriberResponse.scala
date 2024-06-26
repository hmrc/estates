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

package models

import play.api.Logging
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}


sealed trait TaxEnrolmentSubscriberResponse

case object TaxEnrolmentSuccess extends TaxEnrolmentSubscriberResponse
case class TaxEnrolmentFailure(reason: String) extends TaxEnrolmentSubscriberResponse
case object TaxEnrolmentNotProcessed extends TaxEnrolmentSubscriberResponse

object TaxEnrolmentSubscriberResponse extends Logging {

  implicit lazy val httpReads: HttpReads[TaxEnrolmentSubscriberResponse] =
    new HttpReads[TaxEnrolmentSubscriberResponse] {
      override def read(method: String, url: String, response: HttpResponse): TaxEnrolmentSubscriberResponse = {

        logger.info(s"Response status received from tax enrolments: ${response.status}")
        response.status match {
          case NO_CONTENT =>
            TaxEnrolmentSuccess
          case status =>
            val reason = s"Error response from tax enrolments: $status"
            logger.error(reason)
            TaxEnrolmentFailure(reason)
        }
      }
    }
}

