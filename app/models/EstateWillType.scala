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

import play.api.libs.json.{Format, JsPath, Json, OWrites, Writes}
import java.time.LocalDate

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}

case class EstateWillType(name: NameType,
                          dateOfBirth: Option[LocalDate],
                          dateOfDeath: LocalDate,
                          identification: Option[IdentificationType],
                          addressYesNo: Option[Boolean] = None)

object EstateWillType {
  implicit val estateWillTypeFormat: Format[EstateWillType] = Json.format[EstateWillType]

  val ignore: OWrites[Any] = OWrites[Any](_ => Json.obj())

  val estateWillTypeWriteToDes: Writes[EstateWillType] = (
    (JsPath \ "name").write[NameType] and
      (JsPath \ "dateOfBirth").writeNullable[LocalDate] and
      (JsPath \ "dateOfDeath").write[LocalDate] and
      (JsPath \ "identification").writeNullable[IdentificationType] and
      ignore                                                             // We don't want to send this field to DES, but it is required up until now, so are omitting it when building the payload for final registration
    ).apply(unlift(EstateWillType.unapply))
}