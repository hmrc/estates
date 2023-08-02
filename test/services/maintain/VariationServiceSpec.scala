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

package services.maintain

import connectors.BaseConnectorSpec
import models.getEstate.{GetEstateProcessedResponse, ResponseHeader}
import models.variation.{VariationFailureResponse, VariationResponse, VariationSuccessResponse}
import models.{DeclarationForApi, DeclarationName, NameType}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => equalTo}
import play.api.libs.json._
import services._
import uk.gov.hmrc.http.HeaderCarrier
import utils.ErrorResponses.{DuplicateSubmissionErrorResponse, EtmpDataStaleErrorResponse}

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VariationServiceSpec extends BaseConnectorSpec {

  private val formBundleNo = "001234567890"
  private val utr = "1234567890"
  private val internalId = "InternalId"
  private val fullEtmpResponseJson = get4MLDEstateResponse
  private val transformedEtmpResponseJson = Json.parse("""{ "field": "Arbitrary transformed JSON" }""")
  private val estateInfoJson = (fullEtmpResponseJson \ "trustOrEstateDisplay").as[JsValue]
  private val transformedJson = Json.obj("field" -> "value")
  private val transformedJsonWithSubmission = Json.obj("field" -> "value", "submissionDate" -> LocalDate.now())
  private val declarationName = DeclarationName(NameType("Handy", None, "Andy"))
  private val declaration: DeclarationForApi = DeclarationForApi(declarationName, None)

  val estateService = mock[EstatesService]
  val variationsTransformationService = mock[VariationsTransformationService]
  val auditService = mock[AuditService]
  val transformer = mock[VariationDeclarationService]

  val estates5MLDService = new Estates5MLDService()

  before {
    reset(estateService, variationsTransformationService, auditService, transformer)
  }

  def service = new VariationService(
    estateService,
    variationsTransformationService,
    transformer,
    estates5MLDService,
    auditService
  )

  "submitDeclaration" should {

    "submit data correctly when the version matches, and then reset the cache" in {

        val successfulResponse = VariationSuccessResponse("TVN34567890")

        val response = setupForTest(successfulResponse)

        val responseHeader = ResponseHeader("Processed", formBundleNo)

        whenReady(service.submitDeclaration(utr, internalId, declaration)) { variationResponse => {

          variationResponse mustBe successfulResponse

          verify(variationsTransformationService, times( 1))
            .applyDeclarationTransformations(equalTo(utr), equalTo(internalId), equalTo(estateInfoJson))(any[HeaderCarrier])

          verify(transformer, times(1))
            .transform(equalTo(transformedEtmpResponseJson), equalTo(responseHeader), equalTo(response.getEstate), equalTo(declaration))

          verify(auditService).auditVariationSubmitted(
            equalTo(internalId),
            equalTo(transformedJsonWithSubmission),
            equalTo(successfulResponse))(any())

          val arg: ArgumentCaptor[JsValue] = ArgumentCaptor.forClass(classOf[JsValue])

          verify(estateService, times(1)).estateVariation(arg.capture())

          arg.getValue mustBe transformedJsonWithSubmission
        }}

    }

    "audit error when unable to populate personal rep address" in {

      when(variationsTransformationService.populatePersonalRepAddress(any[JsValue]))
        .thenReturn(JsError(__, "no personal rep address"))

      when(estateService.getEstateInfoFormBundleNo(utr))
        .thenReturn(Future.successful(formBundleNo))

      val response: GetEstateProcessedResponse = GetEstateProcessedResponse(estateInfoJson, ResponseHeader("Processed", formBundleNo))

      when(estateService.getEstateInfo(equalTo(utr), equalTo(internalId))(any[HeaderCarrier]()))
        .thenReturn(Future.successful(response))

      val result = service.submitDeclaration(utr, internalId, declaration)

      assert(result.failed.futureValue.getMessage == "There was a problem transforming data for submission to ETMP")
    }

    "audit error when submission fails" in {

      val failedResponse = VariationFailureResponse(DuplicateSubmissionErrorResponse)

      setupForTest(failedResponse)

      whenReady(service.submitDeclaration(utr, internalId, declaration)) { variationResponse => {

        variationResponse mustBe failedResponse

        verify(auditService).auditVariationFailed(
          equalTo(internalId),
          equalTo(transformedJsonWithSubmission),
          equalTo(failedResponse))(any())
      }}
    }
  }


  private def setupForTest(variationResponse: VariationResponse) = {

    when(variationsTransformationService.populatePersonalRepAddress(any[JsValue]))
      .thenReturn(JsSuccess(estateInfoJson))

    when(variationsTransformationService.applyDeclarationTransformations(any(), any(), any())(any[HeaderCarrier]))
      .thenReturn(Future.successful(JsSuccess(transformedEtmpResponseJson)))

    when(estateService.getEstateInfoFormBundleNo(utr))
      .thenReturn(Future.successful(formBundleNo))

    val response: GetEstateProcessedResponse = GetEstateProcessedResponse(estateInfoJson, ResponseHeader("Processed", formBundleNo))

    when(estateService.getEstateInfo(equalTo(utr), equalTo(internalId))(any[HeaderCarrier]()))
      .thenReturn(Future.successful(response))

    when(transformer.transform(any(), any(), any(), any()))
      .thenReturn(JsSuccess(transformedJson))

    when(estateService.estateVariation(any()))
      .thenReturn(Future.successful(variationResponse))

    response
  }

  "Fail if the etmp data version doesn't match our submission data" in {

    when(estateService.getEstateInfoFormBundleNo(utr))
      .thenReturn(Future.successful("31415900000"))

    when(estateService.getEstateInfo(equalTo(utr), equalTo(internalId))(any[HeaderCarrier]()))
      .thenReturn(Future.successful(GetEstateProcessedResponse(estateInfoJson, ResponseHeader("Processed", formBundleNo))))

    whenReady(service.submitDeclaration(utr, internalId, declaration)) { response =>
      response mustBe VariationFailureResponse(EtmpDataStaleErrorResponse)
      verify(estateService, times(0)).estateVariation(any())
    }
  }
}
