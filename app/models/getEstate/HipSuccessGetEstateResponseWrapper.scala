/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.libs.json.{JsError, JsSuccess, JsValue, Reads}

case class HipSuccessGetEstateResponseWrapper(success: GetEstateResponse) extends GetEstateResponse

case object HipSuccessGetEstateResponseWrapper {

  implicit val reads: Reads[HipSuccessGetEstateResponseWrapper] = (json: JsValue) => {
    val header = (json \ "success" \ "responseHeader").asOpt[ResponseHeader]

    header match {
      case Some(parsedHeader) =>
        (json \ "success" \ "trustOrEstateDisplay").toOption match {
          case None    =>
            JsSuccess(HipSuccessGetEstateResponseWrapper(GetEstateStatusResponse(parsedHeader)))
          case Some(x) =>
            x.validate[GetEstate] match {
              case JsSuccess(_, _) =>
                JsSuccess(HipSuccessGetEstateResponseWrapper(GetEstateProcessedResponse(x, parsedHeader)))
              case x: JsError      =>
                JsSuccess(HipSuccessGetEstateResponseWrapper(NotEnoughDataResponse(json, JsError.toJson(x))))
            }
        }
      case None               =>
        JsSuccess(
          HipSuccessGetEstateResponseWrapper(
            NotEnoughDataResponse(json, JsError.toJson(JsError("responseHeader not defined on response")))
          )
        )
    }
  }

}
