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

package transformers.variations

import models.variation.EstatePerRepOrgType
import play.api.libs.json._
import transformers.DeltaTransform

case class AddAmendBusinessPersonalRepTransform(personalRep: EstatePerRepOrgType) extends DeltaTransform with AddAmendPersonalRepTransform {
  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    setPersonalRep(input, Json.toJson(personalRep))
  }
}

object AddAmendBusinessPersonalRepTransform {

  val key = "AddAmendBusinessPersonalRepTransform"

  implicit val format: Format[AddAmendBusinessPersonalRepTransform] = Json.format[AddAmendBusinessPersonalRepTransform]
}


