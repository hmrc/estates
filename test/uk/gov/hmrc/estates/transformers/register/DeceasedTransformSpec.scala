/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.estates.transformers.register

import java.time.LocalDate

import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import uk.gov.hmrc.estates.models.{EstateWillType, IdentificationType, NameType}
import uk.gov.hmrc.estates.utils.JsonUtils

class DeceasedTransformSpec extends FreeSpec with MustMatchers with OptionValues {

  private val newDeceased = EstateWillType(
    NameType("New First", None, "New Last"),
    Some(LocalDate.of(1996, 4, 15)),
    LocalDate.of(2016, 7, 2),
    Some(IdentificationType(
      nino = Some("AA123456B"),
      address = None,
      passport = None
    ))
  )

  "the deceased transform should" - {

    "set the deceased" - {

      "when there is an existing deceased" in {

        val trustJson = JsonUtils.getJsonValueFromFile("valid-estate-registration-01.json")

        val afterJson = JsonUtils.getJsonValueFromFile("transformed/valid-estate-registration-01-deceased-transformed.json")

        val transformer = DeceasedTransform(newDeceased)

        val result = transformer.applyTransform(trustJson).get

        result mustBe afterJson
      }
    }
  }
}