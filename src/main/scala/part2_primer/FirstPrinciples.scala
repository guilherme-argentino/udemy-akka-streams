package part2_primer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

object FirstPrinciples extends App {

  implicit val system = ActorSystem("FirstPrinciples")
  implicit val materialize: ActorMaterializer = ActorMaterializer()

  // sources
  val source = Source(1 to 10)

  // sinks
  val sink = Sink.foreach[Int](println)

  val graph = source.to(sink)
//  graph.run()

  // flow transform elements
  val flow = Flow[Int].map(x => x + 1)
  val sourceWithFlow = source.via(flow)
  val flowWithSink = flow.to(sink)

  sourceWithFlow.to(sink).run()

}
