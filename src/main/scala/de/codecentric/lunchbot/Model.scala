package de.codecentric.lunchbot

case class User(id: String, name: String, realName: Option[String], isBot: Option[Boolean])

case class SlackHandShake(users: List[User], url: String)