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

package transformers.register

import models.YearsReturns
import play.api.libs.json._
import transformers.JsonOperations

case class YearsReturnsTransform(years: YearsReturns)
  extends SetValueAtPathDeltaTransform with JsonOperations {

  override val path: JsPath =  __ \ 'yearsReturns

  override val value: JsValue = Json.toJson(years)
}


object YearsReturnsTransform {

  val key = "YearsReturnsTransform"

  implicit val format: Format[YearsReturnsTransform] = Json.format[YearsReturnsTransform]
}

