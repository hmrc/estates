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

package controllers.transform.variations

import controllers.actions.FakeIdentifierAction
import controllers.transform.variations.personalRep.PersonalRepTransformationController
import models.variation.{EstatePerRepIndType, PersonalRepresentativeType}
import models.{IdentificationType, NameType, Success}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.maintain.PersonalRepTransformationService
import uk.gov.hmrc.auth.core.AffinityGroup.Agent

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PersonalRepTransformationControllerSpec extends AnyFreeSpec with MockitoSugar with ScalaFutures with Matchers {
  private val cc = stubControllerComponents()

  val identifierAction = new FakeIdentifierAction(cc.parsers.default, Agent)

  "amend personal rep" - {

    "must add a new amend personal rep transform" in {
      val personalRepTransformationService = mock[PersonalRepTransformationService]
      val controller = new PersonalRepTransformationController(identifierAction, cc, personalRepTransformationService)

      val newPersonalRepIndInfo = EstatePerRepIndType(
        lineNo = Some("newLineNo"),
        bpMatchStatus = Some("newMatchStatus"),
        name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
        dateOfBirth = LocalDate.of(1965, 2, 10),
        phoneNumber = "newPhone",
        email = Some("newEmail"),
        identification = IdentificationType(Some("newNino"), None, None),
        entityStart = LocalDate.parse("2012-03-14"),
        entityEnd = None
      )

      when(personalRepTransformationService.addAmendPersonalRepTransformer(any(), any(), any()))
        .thenReturn(Future.successful(Success))

      val newPersonalRepInfo = PersonalRepresentativeType(Some(newPersonalRepIndInfo), None)

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(newPersonalRepInfo))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.addOrAmendPersonalRep("aUTR").apply(request)

      status(result) mustBe OK
      verify(personalRepTransformationService).addAmendPersonalRepTransformer("aUTR", "id", newPersonalRepInfo)
    }

    "must return an error for malformed json" in {
      val personalRepTransformationService = mock[PersonalRepTransformationService]
      val controller = new PersonalRepTransformationController(identifierAction, cc, personalRepTransformationService)

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.addOrAmendPersonalRep("aUTR").apply(request)
      status(result) mustBe BAD_REQUEST
    }
  }
}
