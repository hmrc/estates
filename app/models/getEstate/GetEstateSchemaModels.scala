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

package models.getEstate

import models._
import play.api.libs.functional.syntax._
import play.api.libs.json._

import java.time.LocalDate

case class PersonalRepresentativeType (
                                        estatePerRepInd : Option[EstatePerRepIndType] = None,
                                        estatePerRepOrg : Option[EstatePerRepOrgType] = None
                                      )

object PersonalRepresentativeType {

  implicit object PersonalRepReads extends Reads[PersonalRepresentativeType] {

    override def reads(json: JsValue): JsResult[PersonalRepresentativeType] = {
      json.validate[EstatePerRepIndType].map {
        ind =>
          PersonalRepresentativeType(estatePerRepInd = Some(ind))
      }.orElse {
        json.validate[EstatePerRepOrgType].map {
          org =>
            PersonalRepresentativeType(estatePerRepOrg = Some(org))
        }
      }
    }
  }

  implicit val writes : Writes[PersonalRepresentativeType] = Json.writes[PersonalRepresentativeType]
}

case class EstatePerRepIndType(name: NameType,
                               dateOfBirth: LocalDate,
                               identification: IdentificationType,
                               phoneNumber: String,
                               email: Option[String],
                               lineNo: Option[String],
                               bpMatchStatus: Option[String],
                               entityStart: LocalDate)

object EstatePerRepIndType {
  implicit val estatePerRepIndTypeFormat: Format[EstatePerRepIndType] = Json.format[EstatePerRepIndType]
}

case class EstatePerRepOrgType(orgName: String,
                               phoneNumber: String,
                               email: Option[String] = None,
                               identification: IdentificationOrgType,
                               lineNo: Option[String],
                               bpMatchStatus: Option[String],
                               entityStart: LocalDate)

object EstatePerRepOrgType {
  implicit val estatePerRepOrgTypeFormat: Format[EstatePerRepOrgType] = Json.format[EstatePerRepOrgType]
}

case class EstateWillType(name: NameType,
                          dateOfBirth: Option[LocalDate],
                          dateOfDeath: LocalDate,
                          identification: Option[IdentificationType],
                          lineNo: String,
                          bpMatchStatus: Option[String],
                          entityStart: LocalDate)

object EstateWillType {
  implicit val estateWillTypeFormat: Format[EstateWillType] = Json.format[EstateWillType]
}

case class EntitiesType(personalRepresentative: PersonalRepresentativeType,
                        deceased: EstateWillType)

object EntitiesType {
  implicit val entitiesTypeFormat: Format[EntitiesType] = Json.format[EntitiesType]
}

case class Estate(entities: EntitiesType,
                  administrationEndDate: Option[LocalDate],
                  periodTaxDues: String)

object Estate {
  implicit val estateFormat: Format[Estate] = Json.format[Estate]
}

case class GetEstate(matchData: GetMatchData,
                     correspondence: Correspondence,
                     declaration: Declaration,
                     estate: Estate,
                     trustEndDate: Option[LocalDate],
                     submissionDate: Option[LocalDate] // New to 5MLD response, mandatory in 5MLD
                    )

object GetEstate {

  implicit val writes: Writes[GetEstate] = Json.writes[GetEstate]

  implicit val reads: Reads[GetEstate] = (
    (JsPath \ "matchData").read[GetMatchData] and
      (JsPath \ "correspondence").read[Correspondence] and
      (JsPath \ "declaration").read[Declaration] and
      (JsPath \ "details" \ "estate").read[Estate] and
      (JsPath \ "trustEndDate").readNullable[LocalDate] and
      (JsPath \ "submissionDate").readNullable[LocalDate]
    )(GetEstate.apply _)
}
