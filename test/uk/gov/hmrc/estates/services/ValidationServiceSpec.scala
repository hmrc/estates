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

package uk.gov.hmrc.estates.services

import org.scalatest.EitherValues
import uk.gov.hmrc.estates.BaseSpec
import uk.gov.hmrc.estates.models.EstateRegistration
import uk.gov.hmrc.estates.utils.{EstateDataExamples, JsonUtils}


class ValidationServiceSpec extends BaseSpec with EitherValues with EstateDataExamples {

  private lazy val validationService: ValidationService = new ValidationService()
  private lazy val validator : Validator = validationService.get("/resources/schemas/trusts-api-schema-5.0.json")
  private lazy val estateValidator : Validator = validationService.get("/resources/schemas/estates-api-schema-5.0.json")

  "a validator " should {
    "return an empty list of errors when " when {
      "estate payload json having all required fields" in {
        val jsonString = JsonUtils.getJsonFromFile("valid-estate-registration-01.json")

        estateValidator.validate[EstateRegistration](jsonString) must not be 'left
        estateValidator.validate[EstateRegistration](jsonString).right.value mustBe a[EstateRegistration]
      }

      "estate payload json having required fields for estate type 02" in {
        val jsonString = JsonUtils.getJsonFromFile("valid-estate-registration-02.json")

        estateValidator.validate[EstateRegistration](jsonString) must not be 'left
        estateValidator.validate[EstateRegistration](jsonString).right.value mustBe a[EstateRegistration]
      }

      "estate payload json having required fields for estate type 04" in {
        val jsonString = JsonUtils.getJsonFromFile("valid-estate-registration-04.json")

        estateValidator.validate[EstateRegistration](jsonString) must not be 'left
        val rightValue = estateValidator.validate[EstateRegistration](jsonString).right.value

        rightValue mustBe a[EstateRegistration]

        rightValue.estate.entities.personalRepresentative.estatePerRepOrg mustBe defined
        rightValue.estate.entities.deceased.identification mustNot be(defined)
      }
    }

    "return registration domain" when {

      "no personal representative provided" in {
        val jsonString = JsonUtils.getJsonFromFile("invalid-estate-registration-01.json")
        val errorList = estateValidator.validate[EstateRegistration](jsonString).left.get.
          filter(_.message =="object has missing required properties ([\"personalRepresentative\"])")
        errorList.size mustBe 1
      }

      "no correspodence address provided for estate" in {
        val errorList = estateValidator.validate[EstateRegistration](estateWithoutCorrespondenceAddress).left.get.
          filter(_.message =="object has missing required properties ([\"address\"])")
        errorList.size mustBe 1
      }
    }

    "return a list of validaton errors for estates " when {
      "individual personal representative has future date of birth" in {
        val jsonString = JsonUtils.getJsonFromFile("estate-registration-dynamic-01.json").
          replace("{estatePerRepIndDob}", "2030-01-01")
        val errorList =estateValidator.validate[EstateRegistration](jsonString).left.get.
          filter(_.message=="Date of birth must be today or in the past.")
        errorList.size mustBe 1
      }

    }
  }
}