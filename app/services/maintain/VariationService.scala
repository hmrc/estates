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

import exceptions.InternalServerErrorException
import models.DeclarationForApi
import models.getEstate.{EtmpCacheDataStaleResponse, GetEstateProcessedResponse, GetEstateResponse, ResponseHeader}
import models.variation.{VariationFailureResponse, VariationResponse, VariationSuccessResponse}
import play.api.Logging
import play.api.libs.json._
import services.{AuditService, Estates5MLDService, EstatesService, VariationsTransformationService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.ErrorResponses.{EtmpDataStaleErrorResponse, InternalServerErrorErrorResponse}
import utils.JsonOps._
import utils.Session

import javax.inject.Inject
import scala.concurrent.{ExecutionContext,Future}


class VariationService @Inject()(
                                  estatesService: EstatesService,
                                  transformationService: VariationsTransformationService,
                                  declarationService: VariationDeclarationService,
                                  estates5MLDService: Estates5MLDService,
                                  auditService: AuditService)(implicit ec: ExecutionContext) extends Logging {

  def submitDeclaration(utr: String,
                        internalId: String,
                        declaration: DeclarationForApi)
                       (implicit hc: HeaderCarrier): Future[VariationResponse] = {

      getCachedEstateData(utr, internalId) flatMap {
        case cached: GetEstateProcessedResponse =>

          val cachedEstate = cached.getEstate
          val responseHeader: ResponseHeader = cached.responseHeader

          transformationService.populatePersonalRepAddress(cachedEstate) match {
            case JsSuccess(cachedWithAmendedPerRepAddress, _) =>
              submitPopulatedEstate(utr, internalId, cachedWithAmendedPerRepAddress, declaration, responseHeader)
            case e: JsError =>
              auditService.auditVariationTransformationError(
                utr,
                internalId,
                cached.getEstate,
                JsString("Copy address transform"),
                "Failed to populate personal rep address",
                JsError.toJson(e)
              )
              logger.error(s"[submitDeclaration][Session ID: ${Session.id(hc)}][UTR: $utr]" +
                s" Failed to populate personal rep address ${JsError.toJson(e)}")
              Future.failed(InternalServerErrorException("There was a problem transforming data for submission to ETMP"))
          }
        case EtmpCacheDataStaleResponse =>
          Future.successful(VariationFailureResponse(EtmpDataStaleErrorResponse))
        // TODO: Do we need to be more specific?
        case _ =>
          Future.successful(VariationFailureResponse(InternalServerErrorErrorResponse))
      }

  }

  private def submitPopulatedEstate(utr: String,
                                     internalId: String,
                                     cachedWithAmendedPerRepAddress: JsValue,
                                     declaration: DeclarationForApi,
                                     responseHeader: ResponseHeader)
                                    (implicit hc: HeaderCarrier): Future[VariationResponse] = {

      transformationService.applyDeclarationTransformations(utr, internalId, cachedWithAmendedPerRepAddress) flatMap {
        case JsSuccess(transformedDocument, _) =>
          declarationService.transform(
            transformedDocument,
            responseHeader,
            cachedWithAmendedPerRepAddress,
            declaration
          ) flatMap { value =>
            estates5MLDService.applySubmissionDate(value, applySubmissionDate = true)
          } match {
            case JsSuccess(value, _) =>
              logger.debug(s"[submitPopulatedEstate][Session ID: ${Session.id(hc)}][UTR: $utr]" +
                s" submitting variation $value")
              logger.info(s"[submitPopulatedEstate][Session ID: ${Session.id(hc)}][UTR: $utr]" +
                s" successfully transformed json for declaration")
              doSubmit(value, internalId)
            case e: JsError =>
              auditService.auditVariationTransformationError(
                utr,
                internalId,
                transformedDocument,
                transforms = JsString("Declaration transforms"),
                "Problem transforming data for ETMP submission",
                JsError.toJson(e)
              )
              logger.error(s"[submitPopulatedEstate][Session ID: ${Session.id(hc)}][UTR: $utr]" +
                s" Problem transforming data for ETMP submission ${JsError.toJson(e)}")
              Future.failed(InternalServerErrorException("There was a problem transforming data for submission to ETMP"))
          }
        case e: JsError =>
          logger.error(s"[submitPopulatedEstate][Session ID: ${Session.id(hc)}][UTR: $utr]" +
            s" Failed to transform estate info ${JsError.toJson(e)}")
          Future.failed(InternalServerErrorException("There was a problem transforming data for submission to ETMP"))
      }
  }

  private def getCachedEstateData(utr: String, internalId: String)(implicit hc: HeaderCarrier): Future[GetEstateResponse] = {
    for {
      response <- estatesService.getEstateInfo(utr, internalId)
      fbn <- estatesService.getEstateInfoFormBundleNo(utr)
    } yield response match {
      case tpr: GetEstateProcessedResponse if tpr.responseHeader.formBundleNo == fbn =>
        logger.info(s"[getCachedEstateData][Session ID: ${Session.id(hc)}][UTR: $utr]" +
          s" returning GetEstateProcessedResponse")
        response.asInstanceOf[GetEstateProcessedResponse]
      case _: GetEstateProcessedResponse =>
        logger.info(s"[getCachedEstateData][Session ID: ${Session.id(hc)}][UTR: $utr]" +
          s" ETMP cached data in mongo has become stale, rejecting submission")
        EtmpCacheDataStaleResponse
      case _ =>
        logger.warn(s"[getCachedEstateData][Session ID: ${Session.id(hc)}][UTR: $utr] Estate was not in a processed state")
        throw InternalServerErrorException("Submission could not proceed, Estate data was not in a processed state")
    }
  }

  private def doSubmit(value: JsValue, internalId: String)
                      (implicit hc: HeaderCarrier): Future[VariationResponse] = {

    val payload = value.applyRules

    estatesService.estateVariation(payload) map {
      case response: VariationSuccessResponse =>

        logger.info(s"[doSubmit][Session ID: ${Session.id(hc)}] variation submitted")

        auditService.auditVariationSubmitted(internalId, payload, response)

        response

      case response: VariationFailureResponse =>
        logger.error(s"[doSubmit][Session ID: ${Session.id(hc)}] variation failed: ${response.response}")

        auditService.auditVariationFailed(internalId, payload, response)

        response

      case response => response
    }
  }
}
