/*
 * Copyright 2022 HM Revenue & Customs
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

import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import play.api.libs.json.JsString
import utils.JsonUtils

class CorrespondenceNameTransformSpec extends AnyFreeSpec with Matchers with OptionValues {

  val newEstateName = JsString("New Estate Name")

  "the add correspondence name transformer should" - {

    "add a correspondence name" - {

      "when there is an existing correspondence name" in {

        val trustJson = JsonUtils.getJsonValueFromFile("mdtp/valid-estate-registration-01.json")

        val afterJson = JsonUtils.getJsonValueFromFile("transformed/valid-estate-registration-01-correspondence-name-transformed.json")

        val transformer = new CorrespondenceNameTransform(newEstateName)

        val result = transformer.applyTransform(trustJson).get

        result mustBe afterJson
      }

      "when there are no existing correspondence names" in {
        val trustJson = JsonUtils.getJsonValueFromFile("mdtp/valid-estate-registration-01-without-correspondence-details.json")

        val afterJson = JsonUtils.getJsonValueFromFile("transformed/valid-estate-registration-01-correspondence-name-only-transformed.json")

        val transformer = new CorrespondenceNameTransform(newEstateName)

        val result = transformer.applyTransform(trustJson).get

        result mustBe afterJson
      }
    }

  }
}