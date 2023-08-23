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

package transforms

import org.mockito.MockitoSugar
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import play.api.Application
import play.api.libs.json.{JsString, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.repositories.TransformIntegrationTest

class AddCorrespondenceNameSpec extends AsyncWordSpec with Matchers with MockitoSugar with TransformIntegrationTest {

  val newEstateName: JsString = JsString("New Estate Name")
  val newEstateName2: JsString = JsString("New Estate Name 2")

  "an add correspondence name call" must {
    "return added data in a subsequent 'GET' call" in {
          roundTripTest(createApplication, newEstateName)
          roundTripTest(createApplication, newEstateName2)
    }
  }

  private def roundTripTest(app: Application, name: JsString) = {
    val amendRequest = FakeRequest(POST, "/estates/correspondence/name")
      .withBody(Json.toJson(name))
      .withHeaders(CONTENT_TYPE -> "application/json")

    val amendResult = route(app, amendRequest).get
    status(amendResult) mustBe OK

    val newResult = route(app, FakeRequest(GET, "/estates/correspondence/name")).get
    status(newResult) mustBe OK
    contentAsJson(newResult) mustBe Json.obj("name" -> name)
  }
}
