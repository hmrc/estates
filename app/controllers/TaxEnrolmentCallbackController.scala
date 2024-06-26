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

package controllers

import play.api.Logging
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.Session

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class TaxEnrolmentCallbackController @Inject()(
                                               auditConnector :AuditConnector
                                               )(implicit ec: ExecutionContext, cc: ControllerComponents
) extends EstatesBaseController(cc) with Logging {

  def subscriptionCallback() = Action.async(parse.json) {
    implicit request =>
      logger.info(s"[subscriptionCallback][Session ID: ${Session.id(hc)}]" +
        s" Tax-Enrolment: subscription callback message was : ${request.body}")
      Future(Ok(""))
  }

}
