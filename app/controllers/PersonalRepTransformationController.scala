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

import controllers.actions.IdentifierAction
import models.{EstatePerRepIndType, EstatePerRepOrgType}
import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.{LocalDateService, PersonalRepTransformationService}
import utils.{Session, ValidationUtil}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PersonalRepTransformationController @Inject()(
                                          identify: IdentifierAction,
                                          personalRepTransformationService: PersonalRepTransformationService,
                                          cc: ControllerComponents,
                                          localDateService: LocalDateService
                                        )(implicit val executionContext: ExecutionContext
) extends EstatesBaseController(cc) with ValidationUtil with Logging {

  def getPersonalRepInd(): Action[AnyContent] = identify.async {
    implicit request =>
      personalRepTransformationService.getPersonalRepInd(request.identifier) map { personalRep =>
        Ok(
          personalRep map Json.toJson[EstatePerRepIndType] getOrElse Json.obj()
        )
      }
  }

  def getPersonalRepOrg(): Action[AnyContent] = identify.async {
    implicit request =>
      personalRepTransformationService.getPersonalRepOrg(request.identifier) map { personalRep =>
        Ok(
          personalRep map Json.toJson[EstatePerRepOrgType] getOrElse Json.obj()
        )
      }
  }

  def amendPersonalRepInd(): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[EstatePerRepIndType] match {
        case JsSuccess(model, _) =>
          personalRepTransformationService.addAmendEstatePerRepIndTransformer(request.identifier, model) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[Session ID: ${Session.id(hc)}] " +
            s"Supplied Personal Rep could not be read as EstatePerRepIndType - $errors")
          Future.successful(BadRequest)
      }
    }
  }

  def amendPersonalRepOrg(): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      request.body.validate[EstatePerRepOrgType] match {
        case JsSuccess(model, _) =>
          personalRepTransformationService.addAmendEstatePerRepOrgTransformer(request.identifier, model) map { _ =>
            Ok
          }
        case JsError(errors) =>
          logger.warn(s"[Session ID: ${Session.id(hc)}] " +
            s"Supplied Personal Rep could not be read as EstatePerRepIndType - $errors")
          Future.successful(BadRequest)
      }
    }
  }

}
