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
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.time.{Millis, Span}
import play.api.libs.json.{JsResult, JsValue, Json}
import models.getEstate.{GetEstateProcessedResponse, GetEstateResponse}
import models.variation.EstatePerRepIndType
import models.{AddressType, IdentificationType, NameType}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import repositories.VariationsTransformationRepositoryImpl
import services.{AuditService, EstatesService, VariationsTransformationService}
import transformers.ComposedDeltaTransform
import transformers.variations.{AddAmendIndividualPersonalRepTransform, AddCloseEstateTransform}
import utils.{JsonRequests, JsonUtils}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class VariationsTransformationServiceSpec extends AnyFreeSpec with MockitoSugar with ScalaFutures with Matchers with JsonRequests {
  private implicit val pc: PatienceConfig = PatienceConfig(timeout = Span(1000, Millis), interval = Span(15, Millis))


  val unitTestPersonalRepInfo = EstatePerRepIndType(
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

  private val auditService = mock[AuditService]

  private implicit val hc: HeaderCarrier = HeaderCarrier()


  val existingPersonalRepInfo = EstatePerRepIndType(
    lineNo = Some("newLineNo"),
    bpMatchStatus = Some("newMatchStatus"),
    name = NameType("existingFirstName", Some("existingMiddleName"), "existingLastName"),
    dateOfBirth = LocalDate.of(1965, 2, 10),
    phoneNumber = "newPhone",
    email = Some("newEmail"),
    identification = IdentificationType(Some("newNino"), None, None),
    entityStart = LocalDate.parse("2002-03-14"),
    entityEnd = None
  )

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

  "must transform json data with the current transforms" in {
    val repository = mock[VariationsTransformationRepositoryImpl]
    val service = new VariationsTransformationService(repository, mock[EstatesService], auditService)

    val existingTransforms = Seq(
      AddAmendIndividualPersonalRepTransform(unitTestPersonalRepInfo)
    )
    when(repository.get(any(), any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(existingTransforms))))
    when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

    val beforeJson = JsonUtils.getJsonValueFromFile("transformed/variations/estates-personal-rep-transform-before.json")
    val afterJson: JsValue = JsonUtils.getJsonValueFromFile("transformed/variations/estates-personal-rep-transform-after-ind.json")

    val result: Future[JsResult[JsValue]] = service.applyDeclarationTransformations("utr", "internalId", beforeJson)

    whenReady(result) {
      r => r.get mustEqual afterJson
    }
  }
  "must transform json data when no current transforms" in {
    val repository = mock[VariationsTransformationRepositoryImpl]
    val service = new VariationsTransformationService(repository, mock[EstatesService], auditService)

    when(repository.get(any(), any())).thenReturn(Future.successful(None))
    when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

    val beforeJson = JsonUtils.getJsonValueFromFile("transformed/variations/estates-personal-rep-transform-before.json")

    val result: Future[JsResult[JsValue]] = service.applyDeclarationTransformations("utr", "internalId", beforeJson)

    whenReady(result) {
      r => r.get mustEqual beforeJson
    }
  }


  "must apply transformations to ETMP json read from DES service" in {
    val response = get4MLDEstateResponse.as[GetEstateResponse]
    val processedResponse = response.asInstanceOf[GetEstateProcessedResponse]
    val estateService = mock[EstatesService]
    when(estateService.getEstateInfo(any(), any())(any())).thenReturn(Future.successful(response))

    val newPersonalRepIndInfo = EstatePerRepIndType(
      lineNo = None,
      bpMatchStatus = None,
      name = NameType("newFirstName", Some("newMiddleName"), "newLastName"),
      dateOfBirth = LocalDate.of(1965, 2, 10),
      phoneNumber = "newPhone",
      email = Some("newEmail"),
      identification = IdentificationType(
        Some("newNino"),
        None,
        Some(AddressType("newLine1", "newLine2", None, None, Some("NE1 2LA"), "GB"))),
      entityStart = LocalDate.of(2020, 2, 10),
      entityEnd = None
    )

    val existingTransforms = Seq(
      AddAmendIndividualPersonalRepTransform(newPersonalRepIndInfo),
      AddCloseEstateTransform(LocalDate.parse("2020-08-06"))
    )

    val repository = mock[VariationsTransformationRepositoryImpl]

    when(repository.get(any(), any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(existingTransforms))))

    val transformedJson = JsonUtils.getJsonValueFromFile("transformed/variations/valid-get-estate-response-transformed-with-amend-personal-rep-and-close.json")
    val expectedResponse = GetEstateProcessedResponse(transformedJson, processedResponse.responseHeader)

    val service = new VariationsTransformationService(repository, estateService, auditService)

    val result = service.getTransformedData("utr", "internalId")
    whenReady(result) {
      r => r mustEqual expectedResponse
    }
  }

  "addNewTransform" - {

    "must write a corresponding transform to the transformation repository with no existing transforms" in {
      val repository = mock[VariationsTransformationRepositoryImpl]
      val service = new VariationsTransformationService(repository, mock[EstatesService], auditService)

      when(repository.get(any(), any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(Nil))))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addNewTransform("utr", "internalId", AddAmendIndividualPersonalRepTransform(newPersonalRepIndInfo))
      whenReady(result) { _ =>

        verify(repository).set("utr",
          "internalId",
          ComposedDeltaTransform(Seq(AddAmendIndividualPersonalRepTransform(newPersonalRepIndInfo))))
      }
    }

    "must write a corresponding transform to the transformation repository with existing transforms" in {
      val repository = mock[VariationsTransformationRepositoryImpl]
      val service = new VariationsTransformationService(repository, mock[EstatesService], auditService)

      val existingTransforms = Seq(AddAmendIndividualPersonalRepTransform(existingPersonalRepInfo))
      when(repository.get(any(), any())).thenReturn(Future.successful(Some(ComposedDeltaTransform(existingTransforms))))
      when(repository.set(any(), any(), any())).thenReturn(Future.successful(true))

      val result = service.addNewTransform("utr", "internalId", AddAmendIndividualPersonalRepTransform(newPersonalRepIndInfo))
      whenReady(result) { _ =>

        verify(repository).set("utr",
          "internalId",
          ComposedDeltaTransform(Seq(
            AddAmendIndividualPersonalRepTransform(existingPersonalRepInfo),
            AddAmendIndividualPersonalRepTransform(newPersonalRepIndInfo))))
      }
    }
  }

  "remove all transforms" - {

    val utr: String = "utr"
    val internalId: String = "internalId"

    val repository = mock[VariationsTransformationRepositoryImpl]
    val service = new VariationsTransformationService(repository, mock[EstatesService], auditService)

    when(repository.resetCache(any(), any())).thenReturn(Future.successful(Some(Json.obj())))

    val result = service.removeAllTransformations(utr, internalId)

    whenReady(result) { r =>
      verify(repository).resetCache(utr, internalId)
      r.get mustBe Json.obj()
    }
  }
}
