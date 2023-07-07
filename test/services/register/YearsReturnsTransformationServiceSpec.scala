/*
 * Copyright 2023 HM Revenue & Customs
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

package services.register

import models.{YearReturnType, YearsReturns}
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import services.TransformationService
import transformers.ComposedDeltaTransform
import transformers.register.YearsReturnsTransform

import scala.concurrent.Future

class YearsReturnsTransformationServiceSpec extends AnyFreeSpec with MockitoSugar with ScalaFutures with Matchers with OptionValues {

  "YearsReturnsTransformationService" - {

    val cyMinusOneReturn =  YearReturnType(taxReturnYear = "20", taxConsequence = true)
    val cyMinusTwoReturn =  YearReturnType(taxReturnYear = "19", taxConsequence = false)

    "must write a corresponding transform using the transformation service" in {

      val transformationService = mock[TransformationService]
      val service = new YearsReturnsTransformationService(transformationService)

      when(transformationService.addNewTransform(any(), any())).thenReturn(Future.successful(true))

      val result = service.addTransform("internalId", YearsReturns(List(cyMinusOneReturn, cyMinusTwoReturn)))
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform(
          "internalId", YearsReturnsTransform(YearsReturns(List(cyMinusOneReturn, cyMinusTwoReturn)))
        )

      }
    }

    "must not return a transform if one does not exist" - {

      "due to there being no data" in {
        val transformationService = mock[TransformationService]
        val service = new YearsReturnsTransformationService(transformationService)

        when(transformationService.getTransformations(any()))
          .thenReturn(Future.successful(Some(ComposedDeltaTransform(Nil))))

        whenReady(service.get("internalId")) { result =>

          result mustBe None

        }
      }

      "due to there being no transforms" in {
        val transformationService = mock[TransformationService]
        val service = new YearsReturnsTransformationService(transformationService)

        when(transformationService.getTransformations(any()))
          .thenReturn(Future.successful(None))

        whenReady(service.get("internalId")) { result =>
          result mustBe None
        }
      }
    }

    "must return a YearsReturns if retrieved from transforms" - {

      "when there is a single transform" in {

        val transformationService = mock[TransformationService]
        val service = new YearsReturnsTransformationService(transformationService)

        when(transformationService.getTransformations(any()))
          .thenReturn(Future.successful(Some(ComposedDeltaTransform(
            Seq(
              YearsReturnsTransform(YearsReturns(List(cyMinusOneReturn, cyMinusTwoReturn))),
              YearsReturnsTransform(YearsReturns(List(cyMinusOneReturn))))
          ))))

        whenReady(service.get("internalId")) { result =>
          result.value mustBe YearsReturns(List(cyMinusOneReturn))
        }
      }
    }

    "must reset all corresponding transforms using the transformation service" in {

      val transformationService = mock[TransformationService]
      val service = new YearsReturnsTransformationService(transformationService)

      when(transformationService.removeYearsReturnsTransform(any())).thenReturn(Future.successful(true))

      val result = service.removeTransforms("internalId")
      whenReady(result) { _ =>
        verify(transformationService).removeYearsReturnsTransform("internalId")
      }
    }
  }
}
