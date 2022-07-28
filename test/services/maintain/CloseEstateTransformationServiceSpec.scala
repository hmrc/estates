/*
 * Copyright 2022 HM Revenue & Customs
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

package services.maintain

import java.time.LocalDate
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import services.VariationsTransformationService
import transformers.variations.AddCloseEstateTransform
import utils.JsonRequests

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CloseEstateTransformationServiceSpec extends AnyFreeSpec with MockitoSugar with ScalaFutures with Matchers with JsonRequests {

  private val fakeUtr: String = "utr"
  private val internalId: String = "id"
  private val closeDate: LocalDate = LocalDate.parse("2000-01-01")

  "the close estate transformation service" - {

    val transformationService = mock[VariationsTransformationService]
    val service = new CloseEstateTransformationService(transformationService)

    "must add a new close estate transform using the variations transformation service" in {

      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addCloseEstateTransformer(fakeUtr, internalId, closeDate)

      whenReady(result) { _ =>
        verify(transformationService).addNewTransform(
          fakeUtr,
          internalId,
          AddCloseEstateTransform(closeDate)
        )
      }
    }
  }
}
