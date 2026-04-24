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

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.{Schema, SchemaRegistry, SpecificationVersion}
import models.EstateRegistration
import play.api.Logging
import play.api.libs.json._
import utils.EstateBusinessValidation

import javax.inject.Inject
import scala.io.Source
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

class ValidationService @Inject() () {

  private val schemaMapper: ObjectMapper = new ObjectMapper()

  private val schemaRegistry: SchemaRegistry =
    SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_4)

  def get(schemaFile: String): Validator = {
    val resource             = getClass.getResourceAsStream(schemaFile)
    val source               = Source.fromInputStream(resource)
    val schemaJsonFileString = source.mkString
    source.close()
    val schemaJson           = schemaMapper.readTree(schemaJsonFileString)
    val schema               = schemaRegistry.getSchema(schemaJson)
    new Validator(schema, schemaMapper)
  }

}

class Validator(schema: Schema, mapper: ObjectMapper) extends Logging {

//  private val JsonErrorMessageTag  = "message"
//  private val JsonErrorInstanceTag = "instance"
//  private val JsonErrorPointerTag  = "pointer"

  def validate[T](inputJson: String)(implicit reads: Reads[T]): Either[List[EstatesValidationError], T] =
    Try(mapper.readTree(inputJson)) match {
      case Success(json) =>
        val result = schema.validate(json)

        if (result.isEmpty) {
          Json
            .parse(inputJson)
            .validate[T]
            .fold(
              errors => Left(getValidationErrors(errors)),
              request => validateBusinessRules(request)
            )
        } else {
          logger.error(s"[validate] unable to validate to schema")
          Left(getValidationErrors(result))
        }
      case Failure(e)    =>
        logger.error(s"[validate] IOException $e")
        Left(List(EstatesValidationError(s"[Validator][validate] IOException $e", "")))
    }

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

  protected def getValidationErrors(
    errors: Iterable[(JsPath, Iterable[JsonValidationError])]
  ): List[EstatesValidationError] = {
    val validationErrors = errors
      .flatMap(errors => errors._2.map(error => EstatesValidationError(error.message, errors._1.toString())))
      .toList
    logger.debug(s"[getValidationErrors] validationErrors in validate :  $validationErrors")
    validationErrors
  }

  private def getValidationErrors(errors: java.util.List[com.networknt.schema.Error]): List[EstatesValidationError] = {
    val validationErrors = errors.asScala.toList.map { err =>
      val message = err.getMessage
      val loc     = err.getInstanceLocation.toString
      logger.error(s"[getValidationErrors] validation failed at locations :  $loc")
      EstatesValidationError(message, loc)
    }

//    val validationErrors: List[EstatesValidationError] =
//      validationOutput.iterator.asScala.toList.filter(m => m.getLogLevel == ERROR).map { m =>
//        val error     = m.asJson()
//        val message   = error.findValue(JsonErrorMessageTag).asText("")
//        val location  = error.findValue(JsonErrorInstanceTag).at(s"/$JsonErrorPointerTag").asText()
//        val locations = error.findValues(JsonErrorPointerTag)
//        logger.error(s"[getValidationErrors] validation failed at locations :  $locations")
//        EstatesValidationError(message, location)
//      }
    validationErrors
  }

}

case class EstatesValidationError(message: String, location: String)

object EstatesValidationError {
  implicit val formats: Format[EstatesValidationError] = Json.format[EstatesValidationError]
}
