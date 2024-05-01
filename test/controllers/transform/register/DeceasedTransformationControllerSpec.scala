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

package controllers.transform.register

import base.BaseSpec
import controllers.actions.FakeIdentifierAction
import models.{EstateWillType, IdentificationType, NameType}
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.Json
import play.api.mvc.{BodyParsers, ControllerComponents}
import play.api.test.FakeRequest
import play.api.test.Helpers.{CONTENT_TYPE, _}
import services.TransformationService
import transformers.ComposedDeltaTransform
import transformers.register.DeceasedTransform
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation

import java.time.LocalDate
import scala.concurrent.Future

class DeceasedTransformationControllerSpec extends BaseSpec with MockitoSugar with ScalaFutures {

  import scala.concurrent.ExecutionContext.Implicits._

  private implicit val cc: ControllerComponents = injector.instanceOf[ControllerComponents]
  private val bodyParsers = injector.instanceOf[BodyParsers.Default]

  private val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)

  private val mockTransformationService: TransformationService = mock[TransformationService]

  private val previousDeceased = EstateWillType(
    NameType("OldFirst", None, "OldLast"),
    Some(LocalDate.of(1997, 4, 15)),
    LocalDate.of(2018, 7, 2),
    Some(IdentificationType(
      nino = Some("AB111111C"),
      address = None,
      passport = None
    )),
    addressYesNo = None
  )

  private val deceased = EstateWillType(
    NameType("First", None, "Last"),
    Some(LocalDate.of(1996, 4, 15)),
    LocalDate.of(2016, 7, 2),
    Some(IdentificationType(
      nino = Some("AB000000C"),
      address = None,
      passport = None
    )),
    addressYesNo = None
  )

  "deceased transformation controller" when {

    ".get" must {

      "return the deceased person details" in {
        val controller = new DeceasedTransformationController(identifierAction, cc, mockTransformationService)

        when(mockTransformationService.getTransformations(any())).thenReturn(
          Future.successful(Some(ComposedDeltaTransform(Seq(DeceasedTransform(deceased))))))

        val request = FakeRequest("GET", "path")
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.get.apply(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(JSON)
        contentAsJson(result) mustBe Json.parse(
          """
            |{
            | "name": {
            |   "firstName":"First",
            |   "lastName":"Last"
            | },
            | "dateOfBirth":"1996-04-15",
            | "dateOfDeath":"2016-07-02",
            | "identification": {
            |   "nino":"AB000000C"
            | }
            |}
            |""".stripMargin)
      }

      "return the most recent deceased person details" in {
        val controller = new DeceasedTransformationController(identifierAction, cc, mockTransformationService)

        when(mockTransformationService.getTransformations(any())).thenReturn(
          Future.successful(Some(ComposedDeltaTransform(
            Seq(
              DeceasedTransform(previousDeceased),
              DeceasedTransform(deceased)
            )))))

        val request = FakeRequest("GET", "path")
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.get.apply(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(JSON)
        contentAsJson(result) mustBe Json.parse(
          """
            |{
            | "name": {
            |   "firstName":"First",
            |   "lastName":"Last"
            | },
            | "dateOfBirth":"1996-04-15",
            | "dateOfDeath":"2016-07-02",
            | "identification": {
            |   "nino":"AB000000C"
            | }
            |}
            |""".stripMargin)
      }

      "return an empty json object when there is no amount" in {
        val controller = new DeceasedTransformationController(identifierAction, cc, mockTransformationService)

        when(mockTransformationService.getTransformations(any())).thenReturn(Future.successful(None))

        val request = FakeRequest("GET", "path")
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.get.apply(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(JSON)
        contentAsJson(result) mustBe Json.obj()
      }

    }

    ".getDateOfDeath" must {

      "return the deceased person date of death" in {
        val controller = new DeceasedTransformationController(identifierAction, cc, mockTransformationService)

        when(mockTransformationService.getTransformations(any())).thenReturn(
          Future.successful(Some(ComposedDeltaTransform(Seq(DeceasedTransform(deceased))))))

        val request = FakeRequest("GET", "path")
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.getDateOfDeath.apply(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(JSON)
        contentAsJson(result) mustBe Json.parse(
          """
            |"2016-07-02"
            |""".stripMargin)
      }
    }

    ".getIsTaxRequired" must {

      "return true if the date of death is before the current tax year" in {
        val controller = new DeceasedTransformationController(identifierAction, cc, mockTransformationService)

        when(mockTransformationService.getTransformations(any())).thenReturn(
          Future.successful(Some(ComposedDeltaTransform(Seq(DeceasedTransform(deceased))))))

        val request = FakeRequest("GET", "path")
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.getIsTaxRequired.apply(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(JSON)
        contentAsJson(result) mustBe Json.parse(
          """
            |true
            |""".stripMargin)
      }

      "return false if the date of death is in the current tax year" in {
        val deceased = EstateWillType(
          NameType("First", None, "Last"),
          Some(LocalDate.of(1996, 4, 15)),
          LocalDate.now(),
          Some(IdentificationType(
            nino = Some("AB000000C"),
            address = None,
            passport = None
          )),
          addressYesNo = None
        )

        val controller = new DeceasedTransformationController(identifierAction, cc, mockTransformationService)

        when(mockTransformationService.getTransformations(any())).thenReturn(
          Future.successful(Some(ComposedDeltaTransform(Seq(DeceasedTransform(deceased))))))

        val request = FakeRequest("GET", "path")
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.getIsTaxRequired.apply(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(JSON)
        contentAsJson(result) mustBe Json.parse(
          """
            |false
            |""".stripMargin)
      }
    }

    "return false if the transform does not exist" in {
      val controller = new DeceasedTransformationController(identifierAction, cc, mockTransformationService)

      when(mockTransformationService.getTransformations(any())).thenReturn(Future.successful(None))

      val request = FakeRequest("GET", "path")
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.getIsTaxRequired.apply(request)

      status(result) mustBe OK
      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.parse(
        """
          |false
          |""".stripMargin)
    }

    ".save" must {

      "add a transform" in {
        val controller = new DeceasedTransformationController(identifierAction, cc, mockTransformationService)

        val request = FakeRequest("POST", "path")
          .withBody(Json.toJson(deceased))
          .withHeaders(CONTENT_TYPE -> "application/json")

        when(mockTransformationService.addNewTransform(any(), any())).thenReturn(Future.successful(true))

        val result = controller.save().apply(request)

        status(result) mustBe OK
      }

      "must return an error for malformed json" in {
        val controller = new DeceasedTransformationController(identifierAction, cc, mockTransformationService)

        val request = FakeRequest("POST", "path")
          .withBody(Json.toJson("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.save().apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }

  }

}
