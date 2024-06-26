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

package services.maintain

import models.variation.{EstatePerRepIndType, EstatePerRepOrgType, PersonalRepresentativeType}
import models.{IdentificationOrgType, IdentificationType, NameType}
import org.mockito.ArgumentMatchers._
import org.mockito.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{Millis, Span}
import services.VariationsTransformationService
import transformers.variations.AddAmendIndividualPersonalRepTransform
import utils.JsonRequests

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PersonalRepTransformationServiceSpec extends AnyFreeSpec with MockitoSugar with ScalaFutures with Matchers with JsonRequests {
  private implicit val pc: PatienceConfig = PatienceConfig(timeout = Span(1000, Millis), interval = Span(15, Millis))

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

  val newPersonalRepOrgInfo = EstatePerRepOrgType(
    lineNo = Some("newLineNo"),
    bpMatchStatus = Some("newMatchStatus"),
    orgName = "Company Name",
    phoneNumber = "newPhone",
    email = Some("newEmail"),
    identification = IdentificationOrgType(Some("UTR"), None),
    entityStart = LocalDate.parse("2012-03-14"),
    entityEnd = None
  )

  "the amend personal rep transformation service" - {

    "must add a new amend personal rep transform using the variations transformation service" in {

      val transformationService = mock[VariationsTransformationService]
      val service = new PersonalRepTransformationService(transformationService)

      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addAmendPersonalRepTransformer("utr", "internalId", PersonalRepresentativeType(Some(newPersonalRepIndInfo), None))
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("utr",
          "internalId",AddAmendIndividualPersonalRepTransform(newPersonalRepIndInfo))

      }
    }

    "must write a corresponding transform using the transformation service" in {
      val transformationService = mock[VariationsTransformationService]
      val service = new PersonalRepTransformationService(transformationService)

      when(transformationService.addNewTransform(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addAmendPersonalRepTransformer("utr", "internalId", PersonalRepresentativeType(Some(newPersonalRepIndInfo), None))
      whenReady(result) { _ =>

        verify(transformationService).addNewTransform("utr",
          "internalId", AddAmendIndividualPersonalRepTransform(newPersonalRepIndInfo))

      }
    }
  }
}
