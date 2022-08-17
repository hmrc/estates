/*
 * Copyright 2022 HM Revenue & Customs
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

package repositories

import java.sql.Timestamp
import java.time.LocalDateTime

import config.AppConfig
import javax.inject.{Inject, Singleton}
import play.api.Logging
import play.api.libs.json._
import reactivemongo.api.WriteConcern
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import reactivemongo.play.json.collection.JSONCollection
import transformers.ComposedDeltaTransform

import scala.concurrent.{ExecutionContext, Future}

trait TransformationRepository {

  def get(internalId: String): Future[Option[ComposedDeltaTransform]]

  def set(internalId: String, transforms: ComposedDeltaTransform): Future[Boolean]

  def resetCache(internalId: String): Future[Option[JsObject]]
}

@Singleton
class TransformationRepositoryImpl @Inject()(
                            mongo: MongoDriver,
                            config: AppConfig
                          )(implicit ec: ExecutionContext) extends TransformationRepository with Logging {

  private val collectionName: String = "transforms"
  private val cacheTtl = config.ttlInSeconds

  private def collection: Future[JSONCollection] =
    for {
      _ <- ensureIndexes
      res <- Future.successful(mongo.api.collection[JSONCollection](collectionName))
    } yield res


  private val lastUpdatedIndex = Index(
    key = Seq("updatedAt" -> IndexType.Ascending),
    name = Some("transformation-data-updated-at-index"),
    options = BSONDocument("expireAfterSeconds" -> cacheTtl)
  )

  private val idIndex = Index(
    key = Seq("id" -> IndexType.Ascending),
    name = Some("id-index")
  )

  private lazy val ensureIndexes = {
    logger.info("Ensuring collection indexes")
    for {
      collection <- Future.successful(mongo.api.collection[JSONCollection](collectionName))
      createdLastUpdatedIndex <- collection.indexesManager.ensure(lastUpdatedIndex)
      createdIdIndex          <- collection.indexesManager.ensure(idIndex)
    } yield createdLastUpdatedIndex && createdIdIndex
  }

  override def get(internalId: String): Future[Option[ComposedDeltaTransform]] = {

    val selector = Json.obj(
      "id" -> internalId
    )

    collection.flatMap {collection =>

      collection.find(selector, None).one[JsObject].map(opt =>
        for  {
          document <- opt
          transforms <- (document \ "transforms").asOpt[ComposedDeltaTransform]
        } yield transforms)
    }
  }

  override def set(internalId: String, transforms: ComposedDeltaTransform): Future[Boolean] = {

    val selector = Json.obj(
      "id" -> internalId
    )

    val modifier = Json.obj(
      "$set" -> Json.obj(
        "id" -> internalId,
        "updatedAt" -> Json.obj("$date" -> Timestamp.valueOf(LocalDateTime.now())),
        "transforms" -> Json.toJson(transforms)
      )
    )

    collection.flatMap {
      _.update(ordered = false).one(selector, modifier, upsert = true, multi = false).map {
        result => result.ok
      }
    }
  }

  override def resetCache(internalId: String): Future[Option[JsObject]] = {
    val selector = Json.obj(
      "id" -> internalId
    )

    collection.flatMap(_.findAndRemove(selector, None, None, WriteConcern.Default, None, None, Seq.empty).map(
      _.value
    ))
  }
}
