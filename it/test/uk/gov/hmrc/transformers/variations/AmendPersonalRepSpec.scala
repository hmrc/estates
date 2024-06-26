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

package uk.gov.hmrc.transformers.variations

import connectors.EstatesConnector
import models.getEstate.GetEstateResponse
import models.variation.{EstatePerRepIndType, PersonalRepresentativeType}
import models.{AddressType, IdentificationType, NameType}
import org.mockito.ArgumentMatchers._
import org.mockito.MockitoSugar
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.inject.bind
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.repositories.TransformIntegrationTest
import utils.JsonUtils

import java.time.LocalDate
import scala.concurrent.Future

class AmendPersonalRepSpec extends AnyWordSpec with Matchers with MockitoSugar with TransformIntegrationTest {

  val getEstateResponseFromDES: GetEstateResponse = JsonUtils.getJsonValueFromFile("etmp/valid-get-estate-4mld-response.json").as[GetEstateResponse]
  val expectedInitialGetJson: JsValue = JsonUtils.getJsonValueFromFile("it/estates-integration-get-initial.json")

  "an amend personal rep call" should {

    val stubbedEstatesConnector = mock[EstatesConnector]
    when(stubbedEstatesConnector.getEstateInfo(any())).thenReturn(Future.successful(getEstateResponseFromDES))

    val application = appBuilder
      .overrides(
        bind[EstatesConnector].toInstance(stubbedEstatesConnector)
      )
      .build()

    "must return amended data in a subsequent 'get' call" in {

      val newPersonalRepIndInfo = EstatePerRepIndType(
        lineNo = None,
        bpMatchStatus = None,
        name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
        dateOfBirth = LocalDate.of(1965, 2, 10),
        phoneNumber = "newPhone",
        email = Some("newEmail"),
        identification = IdentificationType(
          Some("newNino"),
          None,
          Some(AddressType(
            "1344 Army Road",
            "Suite 111",
            Some("Telford"),
            Some("Shropshire"),
            Some("TF1 5DR"),
            "GB"
          ))),
        entityStart = LocalDate.of(2007, 4, 13),
        entityEnd = None
      )

      val newPersonalRep = PersonalRepresentativeType(Some(newPersonalRepIndInfo), None)

      val expectedGetAfterAmendLeadTrusteeJson: JsValue = JsonUtils.getJsonValueFromFile("it/estates-integration-get-after-amend-personal-rep.json")

      val result = route(application, FakeRequest(GET, "/estates/5174384721/transformed")).get
      status(result) mustBe OK
      contentAsJson(result) mustBe expectedInitialGetJson

      val amendRequest = FakeRequest(POST, "/estates/personal-rep/add-or-amend/5174384721")
        .withBody(Json.toJson(newPersonalRep))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val amendResult = route(application, amendRequest).get
      status(amendResult) mustBe OK

      val newResult = route(application, FakeRequest(GET, "/estates/5174384721/transformed")).get
      status(newResult) mustBe OK
      contentAsJson(newResult) mustBe expectedGetAfterAmendLeadTrusteeJson
    }
  }
}
