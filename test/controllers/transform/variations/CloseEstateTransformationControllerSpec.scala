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
import models.Success
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{CONTENT_TYPE, _}
import services.maintain.CloseEstateTransformationService
import uk.gov.hmrc.auth.core.AffinityGroup.Agent

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CloseEstateTransformationControllerSpec extends AnyFreeSpec with MockitoSugar with ScalaFutures with Matchers {

  private val cc = stubControllerComponents()
  val identifierAction = new FakeIdentifierAction(cc.parsers.default, Agent)

  val fakeUtr: String = "utr"

  "close estate" - {

    val closeEstateTransformationService = mock[CloseEstateTransformationService]
    val controller = new CloseEstateTransformationController(identifierAction, cc, closeEstateTransformationService)

    "must add a new close estate transform" in {

      val closeDate = LocalDate.parse("2000-01-01")

      when(closeEstateTransformationService.addCloseEstateTransformer(any(), any(), any()))
        .thenReturn(Future.successful(Success))

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson(closeDate))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.close(fakeUtr).apply(request)

      status(result) mustBe OK
      verify(closeEstateTransformationService).addCloseEstateTransformer(fakeUtr, "id", closeDate)
    }

    "must return an error for malformed json" in {

      val request = FakeRequest("POST", "path")
        .withBody(Json.toJson("{}"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.close(fakeUtr).apply(request)
      status(result) mustBe BAD_REQUEST
    }
  }
}
