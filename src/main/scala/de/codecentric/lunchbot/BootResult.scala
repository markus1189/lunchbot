package de.codecentric.lunchbot

import akka.Done
import akka.actor.ActorRef
import de.codecentric.lunchbot.actors.Receiver.SlackEndpoint

import scala.concurrent.Future

case class BootResult(slackEndpoint: SlackEndpoint,
                      done: Future[Done],
                      receiverRef: ActorRef)
