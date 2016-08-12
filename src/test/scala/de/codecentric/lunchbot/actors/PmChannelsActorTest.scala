package de.codecentric.lunchbot.actors

import akka.actor.{Actor, Props}
import akka.testkit.TestActorRef
import de.codecentric.lunchbot.actors.PmChannelsActor.ResolveChannel
import de.codecentric.lunchbot.actors.WebApiHandler.OpenIMChannel
import de.codecentric.lunchbot.{DirectMessageChannel, SlackHandShake}

class PmChannelsActorTest extends ActorTest {

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

    val apiHandler = TestActorRef(Props(new MockApiHandler))

    val pmChannelsActor = TestActorRef(PmChannelsActor.props(handshake, apiHandler))
    pmChannelsActor ! ResolveChannel("some guy's ID")

    expectMsg(channel)
  }

  class MockApiHandler extends Actor {
    def receive = {
      case OpenIMChannel(_, forwardTo) =>
        forwardTo ! DirectMessageChannel("1", "some guy's ID")
    }
  }

}
