/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.transformers.variations

import org.mockito.MockitoSugar
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import play.api.Application
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.repositories.TransformIntegrationTest

import java.time.LocalDate

class CloseEstateSpec extends AsyncWordSpec with Matchers with MockitoSugar with TransformIntegrationTest {

  val closeDate1: LocalDate = LocalDate.parse("2000-01-01")
  val closeDate2: LocalDate = LocalDate.parse("2009-12-31")

  "an add close estate call" must {
    "return added data in a subsequent 'GET' call" in {
      roundTripTest(createApplication, closeDate1)
      roundTripTest(createApplication, closeDate2)
    }
  }

  private def roundTripTest(app: Application, date: LocalDate) = {
    val closeRequest = FakeRequest(POST, "/estates/close/utr")
      .withBody(Json.toJson(date))
      .withHeaders(CONTENT_TYPE -> "application/json")

    val closeResult = route(app, closeRequest).get
    status(closeResult) mustBe OK
  }
}
