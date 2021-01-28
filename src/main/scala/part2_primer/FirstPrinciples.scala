package part2_primer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

object FirstPrinciples extends App {

  val system = ActorSystem("FirstPrinciples")
  val materializer = ActorMaterializer()(system)
}
