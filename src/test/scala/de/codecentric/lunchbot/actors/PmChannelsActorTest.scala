package de.codecentric.lunchbot.actors

import akka.actor.{Actor, Props}
import akka.testkit.{TestActorRef, ImplicitSender, TestProbe}
import de.codecentric.lunchbot.actors.PmChannelsActor.ResolveChannel
import de.codecentric.lunchbot.actors.WebApiHandler.OpenIMChannel
import de.codecentric.lunchbot.{DirectMessageChannel, SlackHandShake}
import scala.concurrent.duration._

class PmChannelsActorTest extends ActorTest with ImplicitSender {

  val emptyHandshake = SlackHandShake(List.empty, "", null, List.empty)

  "A PmChannelsActor" should "give a channel read from the Handshake" in {
    val channel = DirectMessageChannel("1", "some guy's ID")
    val handshake = emptyHandshake.copy(ims = List(channel))

    val pmChannelsActor = TestActorRef(PmChannelsActor.props(handshake, testActor))
    pmChannelsActor ! ResolveChannel("some guy's ID")

    expectMsg(channel)
  }

  it should "try the Web API if a channel is not found in the Handshake" in {
    val channel = DirectMessageChannel("1", "some guy's ID")
    val handshake = emptyHandshake

    val apiHandler = TestProbe()

    val pmChannelsActor = TestActorRef(PmChannelsActor.props(handshake, apiHandler.ref))
    pmChannelsActor ! ResolveChannel("some guy's ID")
    apiHandler.expectMsgPF(1.second) {
      case OpenIMChannel(_,replyTo) => replyTo ! channel
    }

    expectMsg(channel)
  }
}
