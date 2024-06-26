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

import base.BaseSpec
import connectors.TaxEnrolmentConnector
import exceptions.{BadRequestException, InternalServerErrorException}
import models.{TaxEnrolmentFailure, TaxEnrolmentSuccess}
import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}


class TaxEnrolmentsServiceSpec extends BaseSpec {

  lazy val mockConnector = mock[TaxEnrolmentConnector]

  lazy val SUT = new TaxEnrolmentsServiceImpl(mockConnector, appConfig)

  before {
    reset(mockConnector)
  }

  ".setSubscriptionId" should {

    "return TaxEnrolmentSuccess  " when {
      "connector returns success taxEnrolmentSuscriberResponse" in {
        when(mockConnector.enrolSubscriber("123456789")).
          thenReturn(Future.successful(TaxEnrolmentSuccess))

        val futureResult = SUT.setSubscriptionId("123456789")

        whenReady(futureResult) {
          result => result mustBe TaxEnrolmentSuccess
        }
        verify(mockConnector, times(1)).enrolSubscriber(any())(any[HeaderCarrier])
      }
    }


    "return TaxEnrolmentFailure " when {

      "tax enrolment returns internal server error" in {
        when(mockConnector.enrolSubscriber("123456789")).
          thenReturn(Future.failed(InternalServerErrorException("")))
        val result = Await.result(SUT.setSubscriptionId("123456789"), Duration.Inf)
        result mustBe a[TaxEnrolmentFailure]
        verify(mockConnector, times(10)).enrolSubscriber(any())(any[HeaderCarrier])
      }
    }

    "return TaxEnrolmentFailure " when {

      "tax enrolment returns error" in {
        when(mockConnector.enrolSubscriber("123456789")).
          thenReturn(Future.failed(BadRequestException))

        val result = Await.result(SUT.setSubscriptionId("123456789"), Duration.Inf)
        result mustBe a[TaxEnrolmentFailure]
        verify(mockConnector, times(10)).enrolSubscriber(any())(any[HeaderCarrier])
      }

    }
  }

}
