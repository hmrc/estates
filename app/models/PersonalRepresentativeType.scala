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

package models

import play.api.libs.json.{Format, Json, Writes}

case class PersonalRepresentativeType (
                                        estatePerRepInd : Option[EstatePerRepIndType] = None,
                                        estatePerRepOrg : Option[EstatePerRepOrgType] = None
                                      )

object PersonalRepresentativeType {
  implicit val personalRepTypeFormat: Format[PersonalRepresentativeType] = Json.format[PersonalRepresentativeType]

  val personalRepTypeWritesToDes: Writes[PersonalRepresentativeType] = Writes {
    personalRepType => personalRepType.estatePerRepInd match {
      case Some(indPerRep) => Json.toJson(indPerRep)
      case None => Json.toJson(personalRepType.estatePerRepOrg)
    }
  }
}
