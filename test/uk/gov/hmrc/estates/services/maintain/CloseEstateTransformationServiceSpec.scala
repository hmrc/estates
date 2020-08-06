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

package uk.gov.hmrc.estates.services.maintain

import java.time.LocalDate

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FreeSpec, MustMatchers}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.estates.services.VariationsTransformationService
import uk.gov.hmrc.estates.transformers.ComposedDeltaTransform
import uk.gov.hmrc.estates.transformers.variations.AddCloseEstateTransform
import uk.gov.hmrc.estates.utils.JsonRequests
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CloseEstateTransformationServiceSpec extends FreeSpec with MockitoSugar with ScalaFutures with MustMatchers with JsonRequests {

  private implicit val hc : HeaderCarrier = HeaderCarrier()

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

    "must check the current transformations and" - {

      "return Some close date if an AddCloseEstateTransform exists" in {

        val date: LocalDate = LocalDate.parse("2000-01-01")

        val transform = AddCloseEstateTransform(date)

        when(transformationService.getTransformations(any(), any()))
          .thenReturn(Future.successful(Some(ComposedDeltaTransform(List(transform)))))

        whenReady(service.getCloseDate(fakeUtr, internalId)) { result =>

          result mustBe Some(date)

        }
      }

      "return None if an AddCloseEstateTransform doesn't exist" in {

        when(transformationService.getTransformations(any(), any()))
          .thenReturn(Future.successful(Some(ComposedDeltaTransform(Nil))))

        whenReady(service.getCloseDate(fakeUtr, internalId)) { result =>

          result mustBe None

        }
      }
    }
  }
}
