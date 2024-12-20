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

package controllers

import play.api.libs.json._
import play.api.mvc.{ControllerComponents, Request, Result}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.ErrorResults._

import scala.concurrent.Future

class EstatesBaseController(cc: ControllerComponents) extends BackendController(cc) {

  protected def matchResponse = """{"match": true}"""

  protected def noMatchResponse = """{"match": false}"""

  protected def withJsonBody[T](f: T => Future[Result])
                                        (implicit request: Request[JsValue],
                                         m: Manifest[T],
                                         reads: Reads[T]) : Future[Result] =
    request.body.validate[T] match {
      case JsSuccess(payload, _) =>
        f(payload)
      case JsError(errs: Iterable[(JsPath, Iterable[JsonValidationError])]) =>
        val response = handleErrorResultByField(errs)
        Future.successful(response)
    }


  def handleErrorResultByField(field: Iterable[(JsPath, Iterable[JsonValidationError])]): Result = {

    val fields = field.map { case (key, validationError) =>
      (key.toString.stripPrefix("/"), validationError.head.message)
    }
    getErrorResult(fields.head._1, fields.head._2)
  }

  def getErrorResult(key: String, error: String): Result = {
    error match {
      case "error.path.missing" =>
        invalidRequestErrorResult
      case _ =>
        errorResults(key)
    }
  }

  protected val errorResults: Map[String, Result] = Map(
    "name" -> invalidNameErrorResult,
    "utr" -> invalidUtrErrorResult,
    "postcode" -> invalidPostcodeErrorResult

  ).withDefaultValue(invalidRequestErrorResult)

}
