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

package utils

import models.EstateRegistration
import services.EstatesValidationError

class EstateDomainValidator(estateRegistration: EstateRegistration) extends ValidationUtil {

  def perRepDobIsNotFutureDate: Option[EstatesValidationError] = {
    val response = estateRegistration.estate.entities.personalRepresentative.estatePerRepInd.map {
      personalRepInd =>
        isNotFutureDate(personalRepInd.dateOfBirth,
          "/estate/entities/personalRepresentative/estatePerRepInd/dateOfBirth", "Date of birth")
    }
    response.flatten
  }

  def personalRepOrgUtrIsNotSameEstateUtr: Option[EstatesValidationError] = {
    val estateUtr = estateRegistration.matchData.map(x => x.utr)
    estateRegistration.estate.entities.personalRepresentative.estatePerRepOrg.flatMap {
      estatePerRepOrg =>
        if (estateUtr.isDefined && (estateUtr == estatePerRepOrg.identification.utr)) {
          Some(EstatesValidationError(s"Personal representative organisation utr is same as estate utr.",
            s"/estate/entities/personalRepresentative/estatePerRepOrg/identification/utr"))
        } else {
          None
        }

    }
  }
}

object EstateBusinessValidation {

  def check(estateRegistration: EstateRegistration): List[EstatesValidationError] = {
    val estateValidator = new EstateDomainValidator(estateRegistration)

    val errorsList = List(
      estateValidator.perRepDobIsNotFutureDate,
      estateValidator.personalRepOrgUtrIsNotSameEstateUtr
    ).flatten

    errorsList
  }
}
