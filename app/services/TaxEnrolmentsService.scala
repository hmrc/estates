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

package services

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.pattern.after
import com.google.inject.ImplementedBy
import config.AppConfig
import connectors.TaxEnrolmentConnector
import models.{TaxEnrolmentFailure, TaxEnrolmentSubscriberResponse}
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import utils.Session

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.control.NonFatal


class TaxEnrolmentsServiceImpl @Inject()(taxEnrolmentConnector: TaxEnrolmentConnector,
                                         config: AppConfig
                                        )(implicit ec: ExecutionContext) extends TaxEnrolmentsService with Logging {

  private val DELAY_SECONDS_BETWEEN_REQUEST = config.delayToConnectTaxEnrolment
  private val MAX_TRIES = config.maxRetry

  override def setSubscriptionId(subscriptionId: String)(implicit hc: HeaderCarrier): Future[TaxEnrolmentSubscriberResponse] = {
    implicit val as: ActorSystem = ActorSystem()
    enrolSubscriberWithRetry(subscriptionId, 1)
  }

  private def enrolSubscriberWithRetry(subscriptionId: String, acc: Int)
                                      (implicit as: ActorSystem, hc: HeaderCarrier): Future[TaxEnrolmentSubscriberResponse] = {
    makeRequest(subscriptionId) recoverWith {
      case NonFatal(_) =>
        if (isMaxRetryReached(acc)) {
          val reason = s"Maximum retry completed. Tax enrolment failed for subscription id $subscriptionId"
          logger.error(s"[enrolSubscriberWithRetry][Session ID: ${Session.id(hc)}] $reason")
          Future.successful(TaxEnrolmentFailure(reason))
        } else {
          after(DELAY_SECONDS_BETWEEN_REQUEST.seconds, as.scheduler){
            logger.error(s"[enrolSubscriberWithRetry][Session ID: ${Session.id(hc)}]" +
              s" Retrying to enrol subscription id $subscriptionId,  $acc")
            enrolSubscriberWithRetry(subscriptionId, acc + 1)
          }
      }
    }
  }

  private def isMaxRetryReached(currentCounter: Int): Boolean =
    currentCounter == MAX_TRIES

  private def makeRequest(subscriptionId: String)(implicit hc: HeaderCarrier): Future[TaxEnrolmentSubscriberResponse] = {
    taxEnrolmentConnector.enrolSubscriber(subscriptionId)
  }

}

@ImplementedBy(classOf[TaxEnrolmentsServiceImpl])
trait TaxEnrolmentsService{
   def setSubscriptionId(subscriptionId: String)(implicit hc: HeaderCarrier): Future[TaxEnrolmentSubscriberResponse]
}
