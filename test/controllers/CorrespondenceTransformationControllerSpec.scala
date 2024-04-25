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

package controllers

import base.BaseSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import play.api.inject.bind
import play.api.libs.json.{JsString, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{CorrespondenceTransformationService, TransformationService}
import transformers.ComposedDeltaTransform
import transformers.register.CorrespondenceNameTransform

import scala.concurrent.Future
import scala.util.Success

class CorrespondenceTransformationControllerSpec extends BaseSpec with MockitoSugar with ScalaFutures {

  private val transformationService = mock[TransformationService]

  val newEstateName = JsString("New Estate Name")

  "add correspondence name" must {

    "add a new amend correspondence name transform" in {

        val correspondenceTransformationService = mock[CorrespondenceTransformationService]

        val application = applicationBuilder()
          .overrides(
            bind[CorrespondenceTransformationService].toInstance(correspondenceTransformationService)
          ).build()

        when(correspondenceTransformationService.addAmendCorrespondenceNameTransformer(any(), any()))
          .thenReturn(Future.successful(Success))

        val controller = application.injector.instanceOf[CorrespondenceTransformationController]

        val request = FakeRequest("POST", "path")
          .withBody(newEstateName)
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.addCorrespondenceName().apply(request)

        status(result) mustBe OK
        verify(correspondenceTransformationService)
          .addAmendCorrespondenceNameTransformer("id", newEstateName)
      }

    "must return an error for malformed json" in {

        val controller = injector.instanceOf[CorrespondenceTransformationController]

        val request = FakeRequest("POST", "path")
          .withBody(Json.parse("{}"))
          .withHeaders(CONTENT_TYPE -> "application/json")

        val result = controller.addCorrespondenceName().apply(request)
        status(result) mustBe BAD_REQUEST
      }
  }

  "getCorrespondenceName" should {

    "return 200 - Ok with processed content" when {
      "a transform is retrieved" in {

        val application = applicationBuilder()
          .overrides(
            bind[TransformationService].toInstance(transformationService)
          ).build()

        when(transformationService.getTransformations(any[String]))
          .thenReturn(Future.successful(Some(ComposedDeltaTransform(Seq(CorrespondenceNameTransform(newEstateName))))))

        val controller = application.injector.instanceOf[CorrespondenceTransformationController]

        val result = controller.getCorrespondenceName()(FakeRequest(GET, "/estates/correspondence/name"))

        status(result) mustBe OK
        contentType(result) mustBe Some(JSON)
        contentAsJson(result) mustBe Json.obj("name" -> newEstateName)

      }

      "a transform is not retrieved" in {

        val application = applicationBuilder()
          .overrides(
            bind[TransformationService].toInstance(transformationService)
          ).build()

        when(transformationService.getTransformations(any[String]))
          .thenReturn(Future.successful(None))

        val controller = application.injector.instanceOf[CorrespondenceTransformationController]

        val result = controller.getCorrespondenceName()(FakeRequest(GET, "/estates/correspondence/name"))

        status(result) mustBe OK
        contentType(result) mustBe Some(JSON)
        contentAsJson(result) mustBe Json.toJson(Json.obj())
      }

    }
  }
}
