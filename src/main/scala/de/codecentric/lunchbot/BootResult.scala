package de.codecentric.lunchbot

import akka.Done
import akka.actor.ActorRef
import actors.MessagesFromSlackReceiver.SlackEndpoint

import scala.concurrent.Future

case class BootResult(slackEndpoint: SlackEndpoint,
                      done: Future[Done],
                      receiverRef: ActorRef)
