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

package controllers.transform.variations

import controllers.EstatesBaseController
import controllers.actions.IdentifierAction
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.VariationsTransformationService

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ClearTransformationsController @Inject()(
                                                identify: IdentifierAction,
                                                cc: ControllerComponents,
                                                variationsTransformationService: VariationsTransformationService
                                              )(implicit val executionContext: ExecutionContext) extends EstatesBaseController(cc) {

  def clearTransformations(utr: String): Action[AnyContent] = identify.async {
    implicit request => {

      variationsTransformationService.removeAllTransformations(utr, request.identifier) map { _ =>
        Ok
      }
    }
  }

}
