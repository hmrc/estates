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
import models.Success
import models.register.AmountOfTaxOwed
import models.register.TaxAmount.AmountMoreThanTenThousand
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.Json
import play.api.mvc.{BodyParsers, ControllerComponents}
import play.api.test.FakeRequest
import play.api.test.Helpers.{CONTENT_TYPE, _}
import services.register.AmountOfTaxTransformationService
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation

import scala.concurrent.Future

class AmountOfTaxOwedTransformationControllerSpec extends BaseSpec with MockitoSugar with ScalaFutures {

  import scala.concurrent.ExecutionContext.Implicits._

  private implicit val cc: ControllerComponents = injector.instanceOf[ControllerComponents]
  private val bodyParsers = injector.instanceOf[BodyParsers.Default]

  val identifierAction = new FakeIdentifierAction(bodyParsers, Organisation)

  val mockTransformationService: AmountOfTaxTransformationService = mock[AmountOfTaxTransformationService]

  "amount of tax owed controller" when {

    ".get" must {

      "return the amount of tax owed" in {
        val controller = new AmountOfTaxOwedTransformationController(identifierAction, cc, LocalDateServiceStub, mockTransformationService)

        when(mockTransformationService.get(any())).thenReturn(Future.successful(Some(AmountOfTaxOwed(AmountMoreThanTenThousand))))

        val request = FakeRequest("GET", "path")
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.get.apply(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(JSON)
        contentAsJson(result) mustBe Json.parse(
          """
            |{
            | "amount": "01"
            |}
            |""".stripMargin)
      }

      "return an empty json object when there is no amount" in {
        val controller = new AmountOfTaxOwedTransformationController(identifierAction, cc, LocalDateServiceStub, mockTransformationService)

        when(mockTransformationService.get(any())).thenReturn(Future.successful(None))

        val request = FakeRequest("GET", "path")
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.get.apply(request)

        status(result) mustBe OK
        contentType(result) mustBe Some(JSON)
        contentAsJson(result) mustBe Json.obj()
      }

    }

    ".save" must {

      "add a transform" in {
        val controller = new AmountOfTaxOwedTransformationController(identifierAction, cc, LocalDateServiceStub, mockTransformationService)

        val amount = AmountOfTaxOwed(AmountMoreThanTenThousand)

        val request = FakeRequest("POST", "path")
          .withBody(Json.toJson(amount))
          .withHeaders(CONTENT_TYPE -> "application/json")

        when(mockTransformationService.addTransform(any(), any())).thenReturn(Future.successful(Success))

        val result = controller.save().apply(request)

        status(result) mustBe OK
      }

      "must return an error for malformed json" in {
        val controller = new AmountOfTaxOwedTransformationController(identifierAction, cc, LocalDateServiceStub, mockTransformationService)

        val request = FakeRequest("POST", "path")
          .withBody(Json.toJson("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.save().apply(request)

        status(result) mustBe BAD_REQUEST
      }

    }

  }

}
