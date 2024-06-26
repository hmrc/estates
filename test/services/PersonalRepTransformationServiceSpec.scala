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
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import transformers.ComposedDeltaTransform
import transformers.register.PersonalRepTransform

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PersonalRepTransformationServiceSpec extends AnyFreeSpec with MockitoSugar with ScalaFutures with Matchers with OptionValues {

  private val personalRepInd = EstatePerRepIndType(
    name =  NameType("First", None, "Last"),
    dateOfBirth = LocalDate.of(2000,1,1),
    identification = IdentificationType(None, None, None),
    phoneNumber = "07987654",
    email = None
  )

  private val personalRepOrg = EstatePerRepOrgType(
    orgName = "Personal Rep",
    identification = IdentificationOrgType(
      utr = None,
      address = None
    ),
    phoneNumber = "07987654345",
    email = None
  )

  "PersonalRepTransformationService" - {

    "must write a corresponding transform using the transformation service" - {
      "for individual" in {

        val transformationService = mock[TransformationService]
        val service = new PersonalRepTransformationService(transformationService)

        when(transformationService.addNewTransform(any(), any())).thenReturn(Future.successful(true))

        val result = service.addAmendEstatePerRepIndTransformer("internalId", personalRepInd)
        whenReady(result) { _ =>

          verify(transformationService).addNewTransform("internalId", PersonalRepTransform(Some(personalRepInd), None))

        }
      }
      "for organisation" in {

        val transformationService = mock[TransformationService]
        val service = new PersonalRepTransformationService(transformationService)

        when(transformationService.addNewTransform(any(), any())).thenReturn(Future.successful(true))

        val result = service.addAmendEstatePerRepOrgTransformer("internalId", personalRepOrg)
        whenReady(result) { _ =>

          verify(transformationService).addNewTransform("internalId", PersonalRepTransform(None, Some(personalRepOrg)))

        }
      }
    }

    "must return a personal rep ind if retrieved from transforms" - {

      "when there is a single transform" in {

        val transformationService = mock[TransformationService]
        val service = new PersonalRepTransformationService(transformationService)

        when(transformationService.getTransformations(any()))
          .thenReturn(Future.successful(Some(ComposedDeltaTransform(Seq(PersonalRepTransform(Some(personalRepInd), None))))))

        whenReady(service.getPersonalRepInd("internalId")) { result =>

          result mustBe Some(personalRepInd)

        }
      }

      "when multiple amend personalRepInd" in {

          val personalRep1 = EstatePerRepIndType(
            name = NameType("First", None, "Last"),
            dateOfBirth = LocalDate.of(2019, 6, 1),
            identification = IdentificationType(
              nino = Some("JH123456C"),
              passport = None,
              address = None
            ),
            phoneNumber = "07987654345",
            email = None
          )
          val personalRep2 = EstatePerRepIndType(
            name = NameType("First", None, "Last"),
            dateOfBirth = LocalDate.of(2019, 6, 1),
            identification = IdentificationType(
              nino = None,
              passport = Some(PassportType("9876217121", LocalDate.of(2010, 1, 1), "UK")),
              address = None
            ),
            phoneNumber = "07987654345",
            email = None
          )

        val transformationService = mock[TransformationService]
        val service = new PersonalRepTransformationService(transformationService)

          when(transformationService.getTransformations(any[String]))
            .thenReturn(Future.successful(Some(ComposedDeltaTransform(
              Seq(
                PersonalRepTransform(Some(personalRep1), None),
                PersonalRepTransform(Some(personalRep2), None)
              )
            ))))

          whenReady(service.getPersonalRepInd("internalId")) { result =>

            result mustBe Some(personalRep2)

          }

        }

      "when personal rep ind within multiple transform types" in {

          val transformationService = mock[TransformationService]
          val service = new PersonalRepTransformationService(transformationService)

          when(transformationService.getTransformations(any[String]))
            .thenReturn(Future.successful(Some(ComposedDeltaTransform(
              Seq(
                PersonalRepTransform(None, Some(personalRepOrg)),
                PersonalRepTransform(Some(personalRepInd), None)
              )
            ))))

          whenReady(service.getPersonalRepInd("internalId")) { result =>

            result mustBe Some(personalRepInd)

          }

        }

      "when personal rep ind superceded" in {

        val transformationService = mock[TransformationService]
        val service = new PersonalRepTransformationService(transformationService)

        when(transformationService.getTransformations(any[String]))
          .thenReturn(Future.successful(Some(ComposedDeltaTransform(
            Seq(
              PersonalRepTransform(Some(personalRepInd), None),
              PersonalRepTransform(None, Some(personalRepOrg))
            )
          ))))

        whenReady(service.getPersonalRepInd("internalId")) { result =>

          result mustBe None

        }

      }

    }

    "must return a personal rep org if retrieved from transforms" - {

      "when there is a single transform" in {

        val transformationService = mock[TransformationService]
        val service = new PersonalRepTransformationService(transformationService)

        when(transformationService.getTransformations(any()))
          .thenReturn(Future.successful(Some(ComposedDeltaTransform(Seq(PersonalRepTransform(None, Some(personalRepOrg)))))))

        whenReady(service.getPersonalRepOrg("internalId")) { result =>

          result mustBe Some(personalRepOrg)

        }
      }

      "when multiple amend personalRepOrg" in {

          val personalRep1 = EstatePerRepOrgType(
            orgName = "Personal Rep 1",
            identification = IdentificationOrgType(
              utr = None,
              address = None
            ),
            phoneNumber = "07987654345",
            email = None
          )
          val personalRep2 = EstatePerRepOrgType(
            orgName = "Personal Rep 2",
            identification = IdentificationOrgType(
              utr = None,
              address = None
            ),
            phoneNumber = "07987654345",
            email = None
          )

        val transformationService = mock[TransformationService]
        val service = new PersonalRepTransformationService(transformationService)

          when(transformationService.getTransformations(any[String]))
            .thenReturn(Future.successful(Some(ComposedDeltaTransform(
              Seq(
                PersonalRepTransform(None, Some(personalRep1)),
                PersonalRepTransform(None, Some(personalRep2)))
            ))))

          whenReady(service.getPersonalRepOrg("internalId")) { result =>

            result mustBe Some(personalRep2)

          }

        }

      "when personal rep org within multiple transform types" in {

          val transformationService = mock[TransformationService]
          val service = new PersonalRepTransformationService(transformationService)

          when(transformationService.getTransformations(any[String]))
            .thenReturn(Future.successful(Some(ComposedDeltaTransform(
              Seq(
                PersonalRepTransform(Some(personalRepInd), None),
                PersonalRepTransform(None, Some(personalRepOrg)))
            ))))

          whenReady(service.getPersonalRepOrg("internalId")) { result =>

            result mustBe Some(personalRepOrg)

          }

        }

    }
    "when personal rep org superceded" in {

      val transformationService = mock[TransformationService]
      val service = new PersonalRepTransformationService(transformationService)

      when(transformationService.getTransformations(any[String]))
        .thenReturn(Future.successful(Some(ComposedDeltaTransform(
          Seq(
            PersonalRepTransform(None, Some(personalRepOrg)),
            PersonalRepTransform(Some(personalRepInd), None))
        ))))

      whenReady(service.getPersonalRepOrg("internalId")) { result =>

        result mustBe None

      }

    }

  }

}
