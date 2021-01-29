package part2_primer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

object BackpressureBasics extends App {

  implicit val system: ActorSystem = ActorSystem("BackpressureBasics")
  implicit val materialize: ActorMaterializer = ActorMaterializer()

  val fastSource = Source(1 to 1000)
  val slowSink = Sink.foreach[Int] { x =>
    // simulate a long processing
    Thread.sleep(1000)
    println(s"Sink: $x")
  }

  //  fastSource.to(slowSink).run() // fusing?!
  // not backpressure

  //  fastSource.async.to(slowSink).run()
  // backpressure

  val simpleFlow = Flow[Int].map { x =>
    println(s"Incoming: $x")
    x + 1
  }

  fastSource.async
    .via(simpleFlow).async
    .to(slowSink).run()
}
