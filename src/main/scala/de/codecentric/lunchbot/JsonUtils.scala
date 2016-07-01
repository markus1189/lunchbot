package de.codecentric.lunchbot

import cats.std.function._
import cats.std.list._
import cats.syntax.traverse._
import cats.free.Trampoline
import io.circe.{Json, JsonObject}

object JsonUtils {

  def snakeCaseToCamelCaseAll(json: Json): Json =
    transformKeys(json, sc2cc).run

  private def sc2cc(in: String) =
    "_([a-z\\d])".r.replaceAllIn(in, _.group(1).toUpperCase)

  private def transformObjectKeys(obj: JsonObject, f: String => String): JsonObject =
    JsonObject.fromIterable(
      obj.toList.map {
        case (k, v) => f(k) -> v
      }
    )

  private def transformKeys(json: Json, f: String => String): Trampoline[Json] =
    json.arrayOrObject(
      Trampoline.done(json),
      _.traverse(j => Trampoline.suspend(transformKeys(j, f))).map(Json.fromValues),
      transformObjectKeys(_, f).traverse(obj => Trampoline.suspend(transformKeys(obj, f))).map(Json.fromJsonObject)
    )
}
