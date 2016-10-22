package de.codecentric.lunchbot

import io.circe._
import io.circe.syntax._
import cats.data.Xor

sealed trait SlackId { def value: String }
case class UserId(value: String) extends SlackId
case class ChannelId(value: String) extends SlackId
case class DmId(value: String) extends SlackId

object SlackId {
  implicit val decoder: Decoder[SlackId] = Decoder.instance { cursor =>
    cursor.as[String].flatMap {
      case s if s.startsWith("U") => Xor.right(UserId(s))
      case s if s.startsWith("C") => Xor.right(ChannelId(s))
      case s if s.startsWith("D") => Xor.right(DmId(s))
      case s => Xor.Left(DecodingFailure(s"Invalid id: $s", List.empty))
    }
  }

  implicit val userDecoder: Decoder[UserId] = Decoder.instance { cursor =>
    cursor.
      as[String].
      ensure(DecodingFailure("Invalid id", List.empty))(_.startsWith("U")).
      map(UserId(_))
  }

  implicit val channelDecoder: Decoder[ChannelId] = Decoder.instance { cursor =>
    cursor.
      as[String].
      ensure(DecodingFailure("Invalid id", List.empty))(_.startsWith("C")).
      map(ChannelId(_))
  }

  implicit val dmDecoder: Decoder[DmId] = Decoder.instance { cursor =>
    cursor.
      as[String].
      ensure(DecodingFailure("Invalid id", List.empty))(_.startsWith("D")).
      map(DmId(_))
  }

  implicit val encoder: Encoder[SlackId] =
    Encoder.instance { sid => Json.fromString(sid.value)  }
}

case class User(id: UserId,
                name: String,
                realName: Option[String],
                isBot: Option[Boolean]) {
  def displayName: String = {
    realName.getOrElse(name)
  }
}

abstract class Channel

case class DirectMessageChannel(id: DmId, user: UserId)

case class SlackHandShake(users: List[User], url: String, self: User, ims: List[DirectMessageChannel])

case class OutgoingSlackMessage(id: Int, channel: SlackId, text: String)

object OutgoingSlackMessage {
  implicit val encoder: Encoder[OutgoingSlackMessage] = Encoder.instance {
    message =>
      Json.fromFields(
          Seq(
              "id" -> Json.fromInt(message.id),
              "channel" -> message.channel.asJson,
              "text" -> Json.fromString(message.text),
              "type" -> Json.fromString("message")
          ))
  }
}

case class IncomingSlackMessage(`type`: String,
                                channel: SlackId,
                                text: String,
                                ts: String,
                                user: UserId,
                                translatedUser: Option[User])
