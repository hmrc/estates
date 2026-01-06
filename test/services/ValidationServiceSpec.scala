/*
 * Copyright 2026 HM Revenue Copyright 2024 HM Revenue & Customs Customs
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

package services

import base.BaseSpec
import models.MatchData.matchDataFormat
import models._
import org.scalatest.EitherValues
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{JsPath, Reads}
import utils.{EstateDataExamples, JsonUtils}

import java.time.LocalDate

class ValidationServiceSpec extends BaseSpec with EitherValues with EstateDataExamples {

  private lazy val validationService: ValidationService = new ValidationService()
  private lazy val estateValidator : Validator = validationService.get("/resources/schemas/4MLD/estates-api-schema-5.0.json")

  "a validator " should {
    "return an empty list of errors when " when {
      "estate payload json having all required fields" in {
        val jsonString = JsonUtils.getJsonFromFile("mdtp/valid-estate-registration-01.json")

        estateValidator.validate[EstateRegistration](jsonString).value mustBe a[EstateRegistration]
      }

      "estate payload json having required fields for estate type 02" in {
        val jsonString = JsonUtils.getJsonFromFile("mdtp/valid-estate-registration-02.json")

        estateValidator.validate[EstateRegistration](jsonString).value mustBe a[EstateRegistration]
      }

      "estate payload json having required fields for estate type 04" in {
        val jsonString = JsonUtils.getJsonFromFile("mdtp/valid-estate-registration-04.json")

        val rightValue = estateValidator.validate[EstateRegistration](jsonString).value

        rightValue mustBe a[EstateRegistration]

        rightValue.estate.entities.personalRepresentative.estatePerRepOrg mustBe defined
        rightValue.estate.entities.deceased.identification mustNot be(defined)
      }
    }

    "return registration domain" when {

      "no personal representative provided" in {
        val jsonString = JsonUtils.getJsonFromFile("mdtp/invalid-estate-registration-01.json")
        val expectedErrors = List("object has missing required properties ([\"personalRepresentative\"])")
        val errorList = estateValidator.validate[EstateRegistration](jsonString).left.value.map(_.message)

        errorList mustBe expectedErrors
      }

      "no correspodence address provided for estate" in {
        val expectedErrors = List("object has missing required properties ([\"address\"])")
        val errorList = estateValidator.validate[EstateRegistration](estateWithoutCorrespondenceAddress).left.value.map(_.message)

        errorList mustBe expectedErrors
      }
    }

    "return a list of validaton errors for estates " when {
      "individual personal representative has future date of birth" in {
        val jsonString = JsonUtils.getJsonFromFile("mdtp/estate-registration-dynamic-01.json")
          .replace("{estatePerRepIndDob}", "2030-01-01")
        val expectedErrors = List("Date of birth must be today or in the past.")
        val errorList = estateValidator.validate[EstateRegistration](jsonString).left.value.map(_.message)

        errorList mustBe expectedErrors
      }

      "valid json provided but fails json read" in {
        val reads: Reads[EstateRegistration] = (
          (JsPath \ "matchData").readNullable[MatchData] and
            (JsPath \ "correspondence").read[Correspondence] and
            (JsPath \ "yearsReturns").readNullable[YearsReturns] and
            (JsPath \ "declaration").read[Declaration] and
            (JsPath \ "details" \ "estate99").read[Estate](Estate.estateFormat) and
            (JsPath \ "agentDetails").readNullable[AgentDetails] and
            (JsPath \ "submissionDate").readNullable[LocalDate]
          ) (EstateRegistration.apply _)

        val jsonString = JsonUtils.getJsonFromFile("mdtp/valid-estate-registration-04.json")
        val expectedErrors = List("error.path.missing")
        val errorList = estateValidator.validate[EstateRegistration](jsonString)(reads).left.value.map(_.message)

        errorList mustBe expectedErrors
      }
    }
  }
}
