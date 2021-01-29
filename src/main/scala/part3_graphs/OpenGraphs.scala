package part3_graphs

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, SinkShape, SourceShape}
import akka.stream.scaladsl.{Broadcast, Concat, GraphDSL, RunnableGraph, Sink, Source}

object OpenGraphs extends App {

  implicit val system: ActorSystem = ActorSystem("OpenGraphs")
  implicit val materialize: ActorMaterializer = ActorMaterializer()

  /*
    A composite source that concatenates 2 sources
    - emits ALL the elements from the first source
    - then ALL the elements from the second
   */
  val firstSource = Source(1 to 10)
  val secondSource = Source(42 to 1000)

  // step 1
  val sourceGraph = Source.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      // step 2: declaring components
      val concat = builder.add(Concat[Int](2))

      // step 3: tying them together
      firstSource ~> concat
      secondSource ~> concat

      // step 4
      SourceShape(concat.out)
    }
  )

  //  sourceGraph.to(Sink.foreach(println)).run()

  /*
    Complex Sink
   */
  val sink1 = Sink.foreach[Int](x => println(s"Meaningful thing 1: $x"))
  val sink2 = Sink.foreach[Int](x => println(s"Meaningful thing 2: $x"))

  val sinkGraph = Sink.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      // step 2: declaring components
      val broadcast = builder.add(Broadcast[Int](2))

      // step 3: tying them together
      broadcast ~> sink1
      broadcast ~> sink2

      // step 4
      SinkShape(broadcast.in)
    }
  )

  //  firstSource.to(sinkGraph).run()

  /**
   * Challenge - complex flow?
   * Write your own flow that's composed of two other flows
   * - one that adds 1 to a number
   * - one that does number * 10
   */
}
