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

import play.api.libs.json.{Format, Json}

case class EstateRegistrationNoDeclaration(matchData: Option[MatchData],
                                           correspondence: CorrespondenceName,
                                           yearsReturns: Option[YearsReturns],
                                           estate: Estate,
                                           agentDetails: Option[AgentDetails] = None)

object EstateRegistrationNoDeclaration {
  implicit val formats : Format[EstateRegistrationNoDeclaration] = Json.format[EstateRegistrationNoDeclaration]
}

case class CorrespondenceName(name: String)

object CorrespondenceName {
  implicit val correspondenceFormat : Format[CorrespondenceName] = Json.format[CorrespondenceName]

}
