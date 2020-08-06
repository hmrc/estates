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

package uk.gov.hmrc.estates.utils

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.mvc.Results._
import uk.gov.hmrc.estates.models.ErrorResponse
import uk.gov.hmrc.estates.utils.VariationErrorResponses._

object VariationErrorResults {

  protected def toBody(response: ErrorResponse): JsValue = Json.toJson(response)

  val invalidNameErrorResult: Result = BadRequest(toBody(InvalidNameErrorResponse))
  val invalidUtrErrorResult: Result = BadRequest(toBody(InvalidUtrErrorResponse))
  val invalidPostcodeErrorResult: Result = BadRequest(toBody(InvalidPostcodeErrorResponse))

  val etmpDataStaleErrorResult: Result = BadRequest(toBody(EtmpDataStaleErrorResponse))

  val invalidRequestErrorResult: Result = BadRequest(toBody(InvalidRequestErrorResponse))
  val invalidCorrelationIdErrorResult: Result = InternalServerError(toBody(InvalidCorrelationIdErrorResponse))
  val duplicateSubmissionErrorResult: Result = Conflict(toBody(DuplicateSubmissionErrorResponse))
  val internalServerErrorErrorResult: Result = InternalServerError(toBody(InternalServerErrorErrorResponse))
  val serviceUnavailableErrorResult: Result = ServiceUnavailable(toBody(ServiceUnavailableErrorResponse))
}
