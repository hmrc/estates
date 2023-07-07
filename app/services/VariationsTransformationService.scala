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

package services

import models.getEstate.{GetEstateProcessedResponse, GetEstateResponse, TransformationErrorResponse}
import play.api.Logging
import play.api.libs.json._
import repositories.VariationsTransformationRepository
import transformers.{ComposedDeltaTransform, DeltaTransform}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VariationsTransformationService @Inject()(transformRepository: VariationsTransformationRepository,
                                                estatesService: EstatesService,
                                                auditService: AuditService) extends Logging {

  def addNewTransform(utr: String, internalId: String, newTransform: DeltaTransform) : Future[Boolean] = {
    transformRepository.get(utr, internalId) map {
      case None =>
        ComposedDeltaTransform(Seq(newTransform))

      case Some(composedTransform) =>
        composedTransform :+ newTransform

    } flatMap { newTransforms =>
      transformRepository.set(utr, internalId, newTransforms)
    } recoverWith {
      case e =>
        logger.error(s"[addNewTransform][UTR: $utr] exception adding new transform: ${e.getMessage}")
        Future.failed(e)
    }
  }

  def getTransformations(utr: String, internalId: String): Future[Option[ComposedDeltaTransform]] =
    transformRepository.get(utr, internalId)

  def removeAllTransformations(utr: String, internalId: String): Future[Option[JsObject]] =
    transformRepository.resetCache(utr, internalId)

  def getTransformedData(utr: String, internalId: String)(implicit hc: HeaderCarrier): Future[GetEstateResponse] = {
    estatesService.getEstateInfo(utr, internalId).flatMap {
      case response: GetEstateProcessedResponse =>
        populatePersonalRepAddress(response.getEstate) match {
          case JsSuccess(fixed, _) =>
            applyTransformations(utr, internalId, fixed).map {
              case JsSuccess(transformed, _) => GetEstateProcessedResponse(transformed, response.responseHeader)
              case JsError(errors) => TransformationErrorResponse(errors.toString)
          }
          case JsError(errors) => Future.successful(TransformationErrorResponse(errors.toString))
        }
      case response => Future.successful(response)
    }
  }

  private def applyTransformations(utr: String, internalId: String, json: JsValue): Future[JsResult[JsValue]] = {
    transformRepository.get(utr, internalId).map {
      case None =>
        JsSuccess(json)
      case Some(transformations) =>
        transformations.applyTransform(json)
    }
  }

  def applyDeclarationTransformations(utr: String, internalId: String, json: JsValue)
                                     (implicit hc : HeaderCarrier): Future[JsResult[JsValue]] = {

    transformRepository.get(utr, internalId).map {
      case None =>
        logger.info(s"[applyDeclarationTransformations][UTR: $utr]no transformations to apply")
        JsSuccess(json)
      case Some(transformations) =>

        logger.debug(s"[applyDeclarationTransformations][UTR: $utr] applying the following transforms $transformations")

        val result = for {
          initial <- {
            logger.info(s"[applyDeclarationTransformations][UTR: $utr] applying transformations")
            transformations.applyTransform(json)
          }
          transformed <- {
            logger.info(s"[applyDeclarationTransformations][UTR: $utr] applying declaration transformations")
            transformations.applyDeclarationTransform(initial)
          }
        } yield transformed

        auditIfError(result, utr, internalId, json, transformations, "Failed to apply declaration transformations.")
    }
  }

  def populatePersonalRepAddress(beforeJson: JsValue): JsResult[JsValue] = {
    val pathToPersonalRepAddress = __ \ 'details \ 'estate \ 'entities \ 'personalRepresentative \ 'identification \ 'address

    if (beforeJson.transform(pathToPersonalRepAddress.json.pick).isSuccess) {
      logger.info(s"[populatePersonalRepAddress] record already has an address for the personal representative, not modifying")
      JsSuccess(beforeJson)
    } else {
      logger.info(s"[populatePersonalRepAddress] record does not have an address for personal rep, adding one from correspondence")
      val pathToCorrespondenceAddress = __ \ 'correspondence \ 'address
      val copyAddress = __.json.update(pathToPersonalRepAddress.json.copyFrom(pathToCorrespondenceAddress.json.pick))
      beforeJson.transform(copyAddress)
    }
  }

  private def auditIfError(result: JsResult[JsValue],
                           utr: String,
                           internalId: String,
                           json: JsValue,
                           transforms: ComposedDeltaTransform,
                           errorReason: String)
                          (implicit hc : HeaderCarrier): JsResult[JsValue] = {
    result match {
      case JsError(e) =>
        auditService.auditVariationTransformationError(
          utr,
          internalId,
          json,
          Json.toJson(transforms),
          errorReason,
          JsError.toJson(e)
        )
        result
      case _ => result
    }
  }
}
