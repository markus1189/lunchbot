package de.codecentric.lunchbot.actors

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.{FlatSpecLike, Matchers}

abstract class ActorTest extends TestKit(ActorSystem("test")) with FlatSpecLike with Matchers
