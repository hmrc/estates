/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.transformers.register

import models.register.AmountOfTaxOwed
import models.register.TaxAmount.{AmountMoreThanFiveHundredThousand, AmountMoreThanTenThousand}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.repositories.TransformIntegrationTest

class AmountTaxOwedSpec extends AnyWordSpec with Matchers with MockitoSugar with TransformIntegrationTest {

  "an add AmountOfTaxOwed call" must {
    "return added data in a subsequent 'GET' call" in {
          roundTripTest(appBuilder.build(), AmountOfTaxOwed(AmountMoreThanTenThousand))
          roundTripTest(appBuilder.build(), AmountOfTaxOwed(AmountMoreThanFiveHundredThousand))
    }
  }

  private def roundTripTest(app: Application, amount: AmountOfTaxOwed) = {
    val amendRequest = FakeRequest(POST, "/estates/amount-tax-owed")
      .withBody(Json.toJson(amount))
      .withHeaders(CONTENT_TYPE -> "application/json")

    val amendResult = route(app, amendRequest).get
    status(amendResult) mustBe OK

    val newResult = route(app, FakeRequest(GET, "/estates/amount-tax-owed")).get
    status(newResult) mustBe OK
    contentAsJson(newResult) mustBe Json.toJson(amount)
  }
}
