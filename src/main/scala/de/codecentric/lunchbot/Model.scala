package de.codecentric.lunchbot

import io.circe.{Json, Encoder}


/*
  {
        "id" : "asdf",
        "team_id" : "asdfasdf",
        "name" : "xxx.yyyyy",
        "deleted" : false,
        "status" : null,
        "color" : "aba727",
        "real_name" : "xxxx yyyyyyyyy",
        "tz" : "Europe/Amsterdam",
        "tz_label" : "Central European Summer Time",
        "tz_offset" : 7200,
        "profile" : {
          "image_24" : "https://avatars.slack-edge.com/2015-12-22/xxxxx_24.jpg",
          "image_32" : "https://avatars.slack-edge.com/2015-12-22/xxxx.jpg",
          "image_48" : "https://avatars.slack-edge.com/2015-12-22/xxx_48.jpg",
          "image_72" : "https://avatars.slack-edge.com/2015-12-22/xxxx_72.jpg",
          "image_192" : "https://avatars.slack-edge.com/2015-12-22/xxxx2093ea1_192.jpg",
          "image_original" : "https://avatars.slack-edge.com/2015-12-22/xxx1_original.jpg",
          "first_name" : "xxxxx",
          "last_name" : "yyyyy",
          "title" : "job desc",
          "skype" : "skype handle",
          "phone" : "1234567890",
          "fields" : {
            "Xf0DAKQUPM" : {
              "value" : "skype handle",
              "alt" : ""
            },
            "Xf0EEHDZH8" : {
              "value" : "job-desc",
              "alt" : ""
            },
            "Xf0EEHD7UY" : {
              "value" : "github handle",
              "alt" : ""
            }
          },
          "image_512" : "https://avatars.slack-edge.com/2015-12-22/xx.jpg",
          "image_1024" : "https://avatars.slack-edge.com/2015-12-22/xxxxx.jpg",
          "avatar_hash" : "fc95461fa090",
          "real_name" : "xxxx yyyy",
          "real_name_normalized" : "xxxxx yyyyy",
          "email" : "xxx.yyyyy@domain.de"
        },
        "is_admin" : false,
        "is_owner" : false,
        "is_primary_owner" : false,
        "is_restricted" : false,
        "is_ultra_restricted" : false,
        "is_bot" : false,
        "presence" : "active"
      }
*/
case class User(id: String, name: String, realName: Option[String], isBot: Option[Boolean])

case class SlackHandShake(users: List[User], url: String)

case class OutgoingSlackMessage(id: Int, channel: String, text: String)

object OutgoingSlackMessage {
  implicit val encoder: Encoder[OutgoingSlackMessage] = Encoder.instance {
    message => Json.fromFields(Seq(
      "id"      -> Json.fromInt(message.id),
      "channel" -> Json.fromString(message.channel),
      "text"    -> Json.fromString(message.text),
      "type"    -> Json.fromString("message")
    ))
  }
}

case class IncomingSlackMessage(`type`: String, channel: String, text: String, timestamp: String)