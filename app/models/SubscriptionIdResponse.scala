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

import exceptions._
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

final case class SubscriptionIdResponse(subscriptionId: String)

object SubscriptionIdResponse extends Logging {

  implicit val formats: OFormat[SubscriptionIdResponse] = Json.format[SubscriptionIdResponse]

  implicit lazy val httpReads: HttpReads[SubscriptionIdResponse] =
    new HttpReads[SubscriptionIdResponse] {
      override def read(method: String, url: String, response: HttpResponse): SubscriptionIdResponse = {
        logger.info(s"Response status received from des: ${response.status}")
        response.status match {
          case OK =>
            response.json.as[SubscriptionIdResponse]
          case BAD_REQUEST =>
            logger.error(s"Bad Request response from des ")
            throw BadRequestException
          case NOT_FOUND =>
            logger.error(s"Not found response from des")
            throw NotFoundException
          case SERVICE_UNAVAILABLE =>
            logger.error("Service unavailable response from des.")
            throw ServiceNotAvailableException("Des dependent service is down.")
          case status =>
            logger.error(s"Error response from des : $status")
            throw InternalServerErrorException(s"Error response from des $status")
        }
      }
    }
}
