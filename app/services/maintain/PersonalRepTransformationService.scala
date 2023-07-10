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

import models.Success
import models.variation.PersonalRepresentativeType
import services.VariationsTransformationService
import transformers.JsonOperations
import transformers.variations.{AddAmendBusinessPersonalRepTransform, AddAmendIndividualPersonalRepTransform}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PersonalRepTransformationService @Inject()(transformationService: VariationsTransformationService
                                                     )(implicit ec: ExecutionContext) extends JsonOperations {

  def addAmendPersonalRepTransformer(utr: String, internalId: String, newPersonalRep: PersonalRepresentativeType): Future[Success.type] = {
    transformationService.addNewTransform(utr, internalId, newPersonalRep match {
      case PersonalRepresentativeType(Some(personalRepInd), None) =>
        AddAmendIndividualPersonalRepTransform(personalRepInd)
      case PersonalRepresentativeType(None, Some(personalRepOrg)) =>
        AddAmendBusinessPersonalRepTransform(personalRepOrg)
    }).map(_ => Success)
  }

}
