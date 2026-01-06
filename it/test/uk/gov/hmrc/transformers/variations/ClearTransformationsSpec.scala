/*
 * Copyright 2026 HM Revenue Copyright 2024 HM Revenue & Customs Customs
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

import connectors.EstatesConnector
import models.getEstate.GetEstateResponse
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.repositories.TransformIntegrationTest
import utils.JsonUtils

import java.time.LocalDate
import scala.concurrent.Future

class ClearTransformationsSpec extends AnyWordSpec with Matchers with MockitoSugar with TransformIntegrationTest {

  val getEstateResponseFromDES: GetEstateResponse = JsonUtils.getJsonValueFromFile("etmp/valid-get-estate-4mld-response.json").as[GetEstateResponse]
  val noTransformsAppliedJson: JsValue = JsonUtils.getJsonValueFromFile("it/estates-integration-get-initial.json")

  "a clear transformations call" should {

    val stubbedEstatesConnector = mock[EstatesConnector]
    when(stubbedEstatesConnector.getEstateInfo(any())).thenReturn(Future.successful(getEstateResponseFromDES))

    val application = appBuilder
      .overrides(
        bind[EstatesConnector].toInstance(stubbedEstatesConnector)
      )
      .build()

    "must return original data in a subsequent 'get' call" in {

      val utr: String = "5174384721"

      val transformRequest = FakeRequest(POST, s"/estates/close/$utr")
        .withBody(Json.toJson(LocalDate.parse("2000-01-01")))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val transformResult = route(application, transformRequest).get
      status(transformResult) mustBe OK

      val initialGetResult = route(application, FakeRequest(GET, s"/estates/$utr/transformed")).get
      status(initialGetResult) mustBe OK

      val clearTransformsRequest = FakeRequest(POST, s"/estates/$utr/clear-transformations")
        .withHeaders(CONTENT_TYPE -> "application/json")

      val clearTransformsResult = route(application, clearTransformsRequest).get
      status(clearTransformsResult) mustBe OK

      val subsequentGetResult = route(application, FakeRequest(GET, s"/estates/$utr/transformed")).get
      status(subsequentGetResult) mustBe OK

      contentAsJson(initialGetResult) mustNot be(noTransformsAppliedJson)
      contentAsJson(subsequentGetResult) mustBe noTransformsAppliedJson
    }
  }
}
