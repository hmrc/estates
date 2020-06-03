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

package uk.gov.hmrc.estates.transformers

import play.api.libs.json.{JsValue, _}

trait DeltaTransform {
  def applyTransform(input: JsValue): JsResult[JsValue]

  def applyDeclarationTransform(input: JsValue): JsResult[JsValue] = JsSuccess(input)
}

object DeltaTransform {

//  private def readsForTransform[T](key: String)(implicit reads: Reads[T]): PartialFunction[JsObject, JsResult[T]] = {
//    case json if json.keys.contains(key) =>
//      (json \ key).validate[T]
//  }

//  def otherIndividualReads: PartialFunction[JsObject, JsResult[DeltaTransform]] = {
//    readsForTransform[AmendOtherIndividualTransform](AmendOtherIndividualTransform.key) orElse
//      readsForTransform[RemoveOtherIndividualsTransform](RemoveOtherIndividualsTransform.key) orElse
//      readsForTransform[AddOtherIndividualTransform](AddOtherIndividualTransform.key)
//  }

  implicit val reads: Reads[DeltaTransform] = Reads[DeltaTransform](
    value =>
//      (
//        trusteeReads orElse
//        beneficiaryReads orElse
//        settlorReads orElse
//        protectorReads orElse
//        otherIndividualReads
//      ) (value.as[JsObject]) orElse
          (throw new Exception(s"Don't know how to deserialise transform"))
  )

//  def addOtherIndividualsWrites[T <: DeltaTransform] : PartialFunction[T, JsValue] = {
//    case transform: AddOtherIndividualTransform =>
//      Json.obj(AddOtherIndividualTransform.key -> Json.toJson(transform)(AddOtherIndividualTransform.format))
//  }

  def defaultWrites[T <: DeltaTransform]: PartialFunction[T, JsValue] = {
    case transform => throw new Exception(s"Don't know how to serialise transform - $transform")
  }

  implicit val writes: Writes[DeltaTransform] = Writes[DeltaTransform] { deltaTransform =>
    (
//      addOtherIndividualsWrites orElse
      defaultWrites
      ).apply(deltaTransform)
  }

}

case class ComposedDeltaTransform(deltaTransforms: Seq[DeltaTransform]) extends DeltaTransform {
  override def applyTransform(input: JsValue): JsResult[JsValue] = {
    deltaTransforms.foldLeft[JsResult[JsValue]](JsSuccess(input))((cur, xform) => cur.flatMap(xform.applyTransform))
  }

  override def applyDeclarationTransform(input: JsValue): JsResult[JsValue] = {
    deltaTransforms.foldLeft[JsResult[JsValue]](JsSuccess(input))((cur, xform) => cur.flatMap(xform.applyDeclarationTransform))
  }

  def :+(transform: DeltaTransform): ComposedDeltaTransform = ComposedDeltaTransform(deltaTransforms :+ transform)
}

object ComposedDeltaTransform {
  implicit val format: Format[ComposedDeltaTransform] = Json.format[ComposedDeltaTransform]
}