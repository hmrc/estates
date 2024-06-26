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

import models.{TaxEnrolmentFailure, TaxEnrolmentSuccess}

class TaxEnrolmentConnectorSpec extends BaseConnectorSpec {

  lazy val connector: TaxEnrolmentConnector = injector.instanceOf[TaxEnrolmentConnector]

  ".enrolSubscriber" should {

    "return TaxEnrolmentSuccess" when {

      "tax enrolments successfully enrols the provided subscription id" in {

        stubForPut(server, "/tax-enrolments/subscriptions/123456789/subscriber", 204)

        val futureResult = connector.enrolSubscriber("123456789")

        whenReady(futureResult) {
          result => result mustBe TaxEnrolmentSuccess
        }
      }
    }

    "return TaxEnrolmentFailure " when {

      "tax enrolments returns bad request " in {

        stubForPut(server, "/tax-enrolments/subscriptions/987654321/subscriber", 400)

        val futureResult = connector.enrolSubscriber("987654321")

        whenReady(futureResult) {
          result => result mustBe TaxEnrolmentFailure("Error response from tax enrolments: 400")
        }
      }

      "tax enrolments returns internal server error " in {

        stubForPut(server, "/tax-enrolments/subscriptions/987654321/subscriber", 500)

        val futureResult = connector.enrolSubscriber("987654321")

        whenReady(futureResult) {
          result => result mustBe TaxEnrolmentFailure("Error response from tax enrolments: 500")
        }
      }
    }
  }
}
