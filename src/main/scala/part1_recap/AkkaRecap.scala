package part1_recap

import akka.actor.{Actor, ActorSystem}

object AkkaRecap extends App {

  class SimpleActor extends Actor {
    override def receive: Receive = {
      case message => println(s"I received: $message")
    }
  }

  // actor encapsulation
  val system = ActorSystem("AkkaRecap")
  new SimpleActor

}
