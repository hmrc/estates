/*
 * Copyright 2020 HM Revenue & Customs
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

package transforms.variations

import java.time.LocalDate

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.{FreeSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.estates.connectors.DesConnector
import uk.gov.hmrc.estates.controllers.actions.{FakeIdentifierAction, IdentifierAction}
import uk.gov.hmrc.estates.models.getEstate.GetEstateResponse
import uk.gov.hmrc.estates.models.variation.{EstatePerRepIndType, PersonalRepresentativeType}
import uk.gov.hmrc.estates.models.{AddressType, IdentificationType, NameType}
import uk.gov.hmrc.estates.utils.JsonUtils
import uk.gov.hmrc.repositories.TransformIntegrationTest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AmendPersonalRepSpec extends FreeSpec with MustMatchers with MockitoSugar with TransformIntegrationTest {

  val getEstateResponseFromDES: GetEstateResponse = JsonUtils.getJsonValueFromFile("etmp/valid-get-estate-response.json").as[GetEstateResponse]
  val expectedInitialGetJson: JsValue = JsonUtils.getJsonValueFromFile("it/estates-integration-get-initial.json")

  "an amend personal rep call" - {

    val stubbedDesConnector = mock[DesConnector]
    when(stubbedDesConnector.getEstateInfo(any())).thenReturn(Future.successful(getEstateResponseFromDES))

    val cc = stubControllerComponents()

    val application = new GuiceApplicationBuilder()
      .configure(Seq(
        "mongodb.uri" -> connectionString,
        "metrics.enabled" -> false,
        "auditing.enabled" -> false,
        "mongo-async-driver.akka.log-dead-letters" -> 0
      ): _*)
      .overrides(
        bind[IdentifierAction].toInstance(new FakeIdentifierAction(cc.parsers.default, Organisation)),
        bind[DesConnector].toInstance(stubbedDesConnector)
      )
      .build()

    "must return amended data in a subsequent 'get' call" in assertMongoTest(application) { app =>

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
        entityStart = LocalDate.of(1965, 2, 10),
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