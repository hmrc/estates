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

import models.{EstatePerRepIndType, EstatePerRepOrgType}
import transformers.ComposedDeltaTransform
import transformers.register.PersonalRepTransform

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class PersonalRepTransformationService @Inject()(
                                                  transformationService: TransformationService
                                                )(implicit val ec: ExecutionContext) {

  def addAmendEstatePerRepIndTransformer(internalId: String, newPersonalRep: EstatePerRepIndType): Future[Success.type] =
    transformationService.addNewTransform(internalId, PersonalRepTransform(Some(newPersonalRep), None)).map(_ => Success)

  def addAmendEstatePerRepOrgTransformer(internalId: String, newPersonalRep: EstatePerRepOrgType): Future[Success.type] =
    transformationService.addNewTransform(internalId, PersonalRepTransform(None, Some(newPersonalRep))).map(_ => Success)

  def getPersonalRepInd(internalId: String): Future[Option[EstatePerRepIndType]] = {
    getMostRecentPerRepTransform(internalId).map {
      case Some(transform) => transform.newPersonalIndRep
      case None => None
    }
  }

  def getPersonalRepOrg(internalId: String): Future[Option[EstatePerRepOrgType]] = {
    getMostRecentPerRepTransform(internalId).map {
      case Some(transform) => transform.newPersonalOrgRep
      case None => None
    }
  }

  private def getMostRecentPerRepTransform(internalId: String): Future[Option[PersonalRepTransform]] = {
    transformationService.getTransformations(internalId) map {
      case Some(ComposedDeltaTransform(transforms)) =>
        transforms.flatMap{
          case transform: PersonalRepTransform => Some(transform)
          case _ => None
        }.lastOption
      case _ => None
    }
  }

}
