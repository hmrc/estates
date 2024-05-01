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

package services.register

import models._
import models.register.RegistrationDeclaration
import models.requests.IdentifierRequest
import play.api.Logging
import play.api.libs.json._
import repositories.TransformationRepository
import services.{AuditService, Estates5MLDService, EstatesService}
import transformers.ComposedDeltaTransform
import transformers.register.DeclarationTransform
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.Session

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegistrationService @Inject()(repository: TransformationRepository,
                                    estateService: EstatesService,
                                    estates5MLDService: Estates5MLDService,
                                    declarationTransformer: DeclarationTransform,
                                    auditService: AuditService
                                   )(implicit ec: ExecutionContext) extends Logging {

  def getRegistration()(implicit request: IdentifierRequest[_], hc: HeaderCarrier): Future[EstateRegistrationNoDeclaration] = {

    repository.get(request.identifier) flatMap {
      case Some(transforms) =>
        buildPrintFromTransforms(transforms) match {
          case JsSuccess(json, _) =>
            json.asOpt[EstateRegistrationNoDeclaration] match {
              case Some(payload) =>
                auditService.auditGetRegistrationSuccess(payload)
                Future.successful(payload)
              case None =>
                val reason = "Unable to parse transformed json as EstateRegistrationNoDeclaration"
                auditService.auditGetRegistrationFailed(transforms, reason)
                Future.failed(new RuntimeException(reason))
            }
          case JsError(errors) =>
            val reason = "Unable to build json from transforms"
            auditService.auditGetRegistrationFailed(transforms, reason, errors.toString)
            Future.failed(new RuntimeException(s"$reason: $errors"))
        }
      case None =>
        val reason = "Unable to get registration due to there being no transforms"
        auditService.auditGetRegistrationFailed(ComposedDeltaTransform(Seq.empty), reason)
        Future.failed(new RuntimeException(reason))
    }

  }

  def submit(declaration: RegistrationDeclaration)(implicit request: IdentifierRequest[_], hc: HeaderCarrier): Future[RegistrationResponse] = {

      repository.get(request.identifier) flatMap {
        case Some(transforms) =>

          buildSubmissionFromTransforms(declaration.name, transforms, applySubmissionDate = true) match {
            case JsSuccess(json, _) =>

              json.asOpt[EstateRegistration] match {
                case Some(payload) =>
                  submitAndAuditResponse(payload)
                case None =>
                  logger.warn(s"[submit][Session ID: ${Session.id(hc)}]" +
                    s" unable to send registration for session due to being unable to validate as EstateRegistration")

                  val reason = "Unable to parse transformed json as EstateRegistration"

                  auditService.auditRegistrationTransformationError(
                    data = json,
                    transforms = Json.toJson(transforms),
                    errorReason = reason
                  )
                  Future.failed(new RuntimeException(reason))
              }
            case JsError(errors) =>

              logger.warn(s"[submit][Session ID: ${Session.id(hc)}] unable to build submission payload for session")

              val reason = "Unable to build json from transforms"

              auditService.auditRegistrationTransformationError(
                transforms = Json.toJson(transforms),
                errorReason = reason,
                jsErrors = errors.toString()
              )
              Future.failed(new RuntimeException(s"$reason: $errors"))
          }
        case None =>

          logger.warn(s"[submit][Session ID: ${Session.id(hc)}]" +
            s" unable to send registration for session due to there being no data in mongo")

          val reason = "Unable to submit registration due to there being no transforms"

          auditService.auditRegistrationTransformationError(errorReason = reason)

          Future.failed(new RuntimeException(reason))
      }

  }

  private def submitAndAuditResponse(payload: EstateRegistration)
                                    (implicit request: IdentifierRequest[_], hc: HeaderCarrier) : Future[RegistrationResponse] = {

    estateService.registerEstate(payload) map {
      case r@RegistrationTrnResponse(trn) =>

        logger.info(s"[submitAndAuditResponse][Session ID: ${Session.id(hc)}] submission for session received TRN $trn")

        auditService.auditRegistrationSubmitted(payload, trn)
        r
      case r: RegistrationFailureResponse =>

        logger.error(s"[submitAndAuditResponse][Session ID: ${Session.id(hc)}]" +
          s" submission for session was unable to be submitted due to status ${r.status} ${r.code} and ${r.message}")

        auditService.auditRegistrationFailed(request.identifier, Json.toJson(payload), r)
        r
    }
  }

  private def buildPrintFromTransforms(transforms: ComposedDeltaTransform): JsResult[JsValue] = {
    for {
      result <- applyTransforms(transforms)
    } yield result
  }

  def buildSubmissionFromTransforms(name: NameType, transforms: ComposedDeltaTransform, applySubmissionDate: Boolean)
                                   (implicit request: IdentifierRequest[_]): JsResult[JsValue] = {
    for {
      transformsApplied <- applyTransforms(transforms)
      declarationTransformsApplied <- applyTransformsForDeclaration(transforms, transformsApplied)
      result <- applyDeclarationAddressTransform(declarationTransformsApplied, request.affinityGroup, name)
      resultWithSubmissionDate <- estates5MLDService.applySubmissionDate(result, applySubmissionDate)
    } yield {
      resultWithSubmissionDate
    }
  }

  private def applyTransforms(transforms: ComposedDeltaTransform): JsResult[JsValue] = {
    logger.info(s"[applyTransforms] applying transformations")
    transforms.applyTransform(Json.obj())
  }

  private def applyTransformsForDeclaration(transforms: ComposedDeltaTransform, original: JsValue): JsResult[JsValue] = {
    logger.info(s"[applyTransformsForDeclaration] applying declaration transformations")
    transforms.applyDeclarationTransform(original)
  }

  private def applyDeclarationAddressTransform(original: JsValue,
                                               affinityGroup: AffinityGroup,
                                               name: NameType): JsResult[JsValue] = {
    logger.info(s"[applyDeclarationAddressTransform] applying declaration address transform")
    declarationTransformer.transform(affinityGroup, original, name)
  }

}
