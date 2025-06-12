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

package services

import models._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{Millis, Span}
import org.scalatestplus.mockito.MockitoSugar
import repositories.TransformationRepositoryImpl
import transformers.ComposedDeltaTransform
import transformers.register.{PersonalRepTransform, YearsReturnsTransform}

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TransformationServiceSpec extends AnyFreeSpec with MockitoSugar with ScalaFutures with Matchers {

  private implicit val pc: PatienceConfig = PatienceConfig(timeout = Span(1000, Millis), interval = Span(15, Millis))

  "addNewTransform" - {

    "must write a corresponding transform to the transformation repository" - {
      "with no existing transforms" in {

        val repository = mock[TransformationRepositoryImpl]
        val service = new TransformationService(repository)

        val personalRep = EstatePerRepIndType(
          name = NameType("First", None, "Last"),
          dateOfBirth = LocalDate.of(2000, 1, 1),
          identification = IdentificationType(None, None, None),
          phoneNumber = "07987654",
          email = None
        )

        val transform = PersonalRepTransform(Some(personalRep), None)

        when(repository.get(any())).thenReturn(Future.successful(None))
        when(repository.set(any(), any())).thenReturn(Future.successful(true))

        val result = service.addNewTransform("internalId", transform)

        whenReady(result) { _ =>

          verify(repository).set(
            "internalId",
            ComposedDeltaTransform(Seq(transform))
          )
        }

      }

      "with existing transforms" in {

        val repository = mock[TransformationRepositoryImpl]
        val service = new TransformationService(repository)

        val personalRep = EstatePerRepIndType(
          name = NameType("First", None, "Last"),
          dateOfBirth = LocalDate.of(2000, 1, 1),
          identification = IdentificationType(None, None, None),
          phoneNumber = "07987654",
          email = None
        )

        val existingTransforms = Seq(PersonalRepTransform(Some(personalRep), None))

        val newTransform = PersonalRepTransform(
          Some(personalRep.copy(email = Some("e@mail.com"))), None
        )

        when(repository.get(any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(existingTransforms))))
        when(repository.set(any(), any())).thenReturn(Future.successful(true))

        val result = service.addNewTransform("internalId", newTransform)

        whenReady(result) { _ =>

          verify(repository).set(
            "internalId",
            ComposedDeltaTransform(existingTransforms :+ newTransform)
          )
        }

      }

      "return a failure if unable to get transforms" in {

        val repository = mock[TransformationRepositoryImpl]
        val service = new TransformationService(repository)

        when(repository.get(any())).thenReturn(Future.failed(new RuntimeException))

        val personalRep = EstatePerRepIndType(
          name = NameType("First", None, "Last"),
          dateOfBirth = LocalDate.of(2000, 1, 1),
          identification = IdentificationType(None, None, None),
          phoneNumber = "07987654",
          email = None
        )

        val newTransform = PersonalRepTransform(
          Some(personalRep.copy(email = Some("e@mail.com"))), None
        )

        val result = service.addNewTransform("internalId", newTransform)

        result.failed.futureValue mustBe a[RuntimeException]

      }
    }

  }

  "removeYearsReturnsTransform" - {

    "must remove all YearsReturnsTransforms from the transformation repository" - {
      "with no existing transforms" in {

        val repository = mock[TransformationRepositoryImpl]
        val service = new TransformationService(repository)

        when(repository.get(any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(Nil))))
        when(repository.set(any(), any())).thenReturn(Future.successful(true))

        val result = service.removeYearsReturnsTransform("internalId")

        whenReady(result) { _ =>

          verify(repository).set(
            "internalId",
            ComposedDeltaTransform(Seq())
          )
        }

      }

      "with existing transforms" in {

        val repository = mock[TransformationRepositoryImpl]
        val service = new TransformationService(repository)

        val personalRep = EstatePerRepIndType(
          name = NameType("First", None, "Last"),
          dateOfBirth = LocalDate.of(2000, 1, 1),
          identification = IdentificationType(None, None, None),
          phoneNumber = "07987654",
          email = None
        )

        val existingTransforms = Seq(
          PersonalRepTransform(Some(personalRep), None),
          YearsReturnsTransform(YearsReturns(List(YearReturnType("19", taxConsequence = true))))
        )

        when(repository.get(any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(existingTransforms))))
        when(repository.set(any(), any())).thenReturn(Future.successful(true))

        val result = service.removeYearsReturnsTransform("internalId")

        whenReady(result) { _ =>

          verify(repository).set(
            "internalId",
            ComposedDeltaTransform(Seq(PersonalRepTransform(Some(personalRep), None)))
          )
        }

      }

      "with only YearsReturns existing transform" in {

        val repository = mock[TransformationRepositoryImpl]
        val service = new TransformationService(repository)

        val existingTransforms = Seq(
          YearsReturnsTransform(YearsReturns(List(YearReturnType("19", taxConsequence = false)))),
          YearsReturnsTransform(YearsReturns(List(YearReturnType("19", taxConsequence = true))))
        )

        when(repository.get(any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(existingTransforms))))
        when(repository.set(any(), any())).thenReturn(Future.successful(true))

        val result = service.removeYearsReturnsTransform("internalId")

        whenReady(result) { _ =>

          verify(repository).set(
            "internalId",
            ComposedDeltaTransform(Seq())
          )
        }

      }

      "return failure if unable to get transforms" in {

        val repository = mock[TransformationRepositoryImpl]
        val service = new TransformationService(repository)

        when(repository.get(any())).thenReturn(Future.failed(new RuntimeException))
        val result = service.removeYearsReturnsTransform("internalId")

        result.failed.futureValue mustBe a[RuntimeException]
      }
    }

  }

  "getTransformations" - {

    "return existing transformations" in {
      val repository = mock[TransformationRepositoryImpl]
      val service = new TransformationService(repository)

      when(repository.get(any())).thenReturn(Future.successful(None))

      val result = service.getTransformations("internalId")

      whenReady(result) { r =>
        r mustBe None
      }
    }

  }

  "removeAllTransformations" - {

    "reset cache" in {
      val repository = mock[TransformationRepositoryImpl]
      val service = new TransformationService(repository)

      when(repository.resetCache(any())).thenReturn(Future.successful(None))

      val result = service.removeAllTransformations("internalId")

      whenReady(result) { r =>
        r mustBe None
      }
    }

  }
}
