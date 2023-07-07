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

package utils

import models.EstateRegistration
import play.api.libs.json.__


trait EstateDataExamples  extends  JsonRequests {


  def estatePerRepIndWithValues(estatePerRepIndDob : String ="2001-01-01"): EstateRegistration = {
    val json = getJsonValueFromFile("mdtp/estate-registration-dynamic-01.json")
    getJsonValueFromString(json.toString().
      replace("{estatePerRepIndDob}", estatePerRepIndDob)).validate[EstateRegistration].get
  }

  def estatePerRepOrgWithValues (estatePerRepOrgUtr: String ="1234567890"): EstateRegistration = {
    val json = getJsonValueFromFile("estate-registration-org-dynamic-01.json")
    getJsonValueFromString(json.toString().
      replace("{estatePerRepOrgUtr}", estatePerRepOrgUtr)).validate[EstateRegistration].get
  }

  def estateWithoutCorrespondenceAddress : String = {
    val json = getJsonValueFromFile("mdtp/valid-estate-registration-01.json")
    val jsonTransformer = (__  \ 'correspondence \  'address ).json.prune
    json.transform(jsonTransformer).get.toString()
  }

}
