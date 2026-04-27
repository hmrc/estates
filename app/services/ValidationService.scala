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

package services

import com.networknt.schema.{Error, InputFormat, Schema, SchemaRegistry, SpecificationVersion}
import models.EstateRegistration
import play.api.Logging
import play.api.libs.json._
import utils.EstateBusinessValidation

import java.io.InputStream
import javax.inject.Inject
import scala.io.Source
import scala.jdk.CollectionConverters._

class ValidationService @Inject() {

  def get(schemaFile: String): Validator = {
    val resource = resourceAsString(schemaFile)
      .getOrElse(throw new RuntimeException("Missing schema: " + schemaFile))

    val schema = SchemaRegistry
      .withDefaultDialect(SpecificationVersion.DRAFT_4)
      .getSchema(resource, InputFormat.JSON)
    new Validator(schema)
  }

  private def resourceAsString(resourcePath: String): Option[String] =
    resourceAsInputStream(resourcePath) map { is =>
      Source.fromInputStream(is).getLines().mkString("\n")
    }

  private def resourceAsInputStream(resourcePath: String): Option[InputStream] =
    Option(getClass.getResourceAsStream(resourcePath))

}

class Validator(schema: Schema) extends Logging {

  def validate[T](inputJson: String)(implicit reads: Reads[T]): Either[List[EstatesValidationError], T] = {
    val errorsNew: List[Error] = validateInternal(inputJson)
    if (errorsNew.isEmpty) {
      Json
        .parse(inputJson)
        .validate[T]
        .fold(
          errors => Left(getValidationErrorsForJSPath(errors)),
          request => validateBusinessRules(request)
        )
    } else {
      logger.error(s"[validate] unable to validate to schema")
      Left(getValidationErrors(errorsNew))
    }

  }

  private def validateInternal(subject: String): List[Error] =
    schema.validate(subject, InputFormat.JSON).asScala.toList

  private def validateBusinessRules[T](request: T): Either[List[EstatesValidationError], T] =
    request match {
      case estateRegistration: EstateRegistration =>
        EstateBusinessValidation.check(estateRegistration) match {
          case Nil             => Right(request)
          case errors @ _ :: _ =>
            logger.error(s"[validateBusinessRules] Validation fails : $errors")
            Left(errors)
        }
      case _                                      => Right(request)
    }

  private def getValidationErrorsForJSPath(
    errors: Iterable[(JsPath, Iterable[JsonValidationError])]
  ): List[EstatesValidationError] = {
    val validationErrors = errors
      .flatMap(errors => errors._2.map(error => EstatesValidationError(error.message, errors._1.toString())))
      .toList
    logger.debug(s"[getValidationErrors] validationErrors in validate :  $validationErrors")
    validationErrors
  }

  private def getValidationErrors(errors: List[Error]): List[EstatesValidationError] = {
    val validationErrors = errors.map { err =>
      val message = err.getMessage
      val loc     = err.getInstanceLocation.toString
      logger.error(s"[getValidationErrors] validation failed at locations :  $loc")
      EstatesValidationError(message, loc)
    }
    validationErrors
  }

}

case class EstatesValidationError(message: String, location: String)

object EstatesValidationError {
  implicit val formats: Format[EstatesValidationError] = Json.format[EstatesValidationError]
}
