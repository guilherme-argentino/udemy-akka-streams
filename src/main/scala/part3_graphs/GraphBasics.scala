package part3_graphs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source, Zip}

object GraphBasics extends App {

  implicit val system: ActorSystem = ActorSystem("GraphBasics")
  implicit val materialize: ActorMaterializer = ActorMaterializer()

  val input = Source(1 to 1000)
  val incrementer = Flow[Int].map(x => x + 1)
  val multiplier = Flow[Int].map(x => x * 10)
  val output = Sink.foreach[(Int, Int)](println)

  // step 1 - setting up the fundamentals for the graph
  val graph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] => // builder = MUTABLE data structure
      import GraphDSL.Implicits._ // brings some nice operators into scope

      // step 2 - add the necessary components of this graph
      val broadcast = builder.add(Broadcast[Int](2)) // fan-out operator
      val zip = builder.add(Zip[Int, Int]) // fan-in operator

      // step 3 - tying up the components
      input ~> broadcast

      broadcast.out(0) ~> incrementer ~> zip.in0
      broadcast.out(1) ~> multiplier ~> zip.in1

      zip.out ~> output

      // step 4 - return a closed shape
      ClosedShape // FREEZE the builder's shape
      // shape
    } // graph
  ) // runnable graph

  //  graph.run() // run the graph and materialize it

  /**
   * exercise 1: feed a source into 2 sinks at the same time (hint: use a broadcast)
   */

  RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[Int](2))

      input ~> broadcast

      broadcast.out(0) ~> Sink.foreach[Int](println)
      broadcast.out(1) ~> Sink.foreach[Int](println)

      ClosedShape

    }
  ).run()

  val firstSink = Sink.foreach[Int](x => println(s"First Sink: $x"))
  val secondSink = Sink.foreach[Int](x => println(s"Second Sink: $x"))

  // step 1
  val sourceToTwoSinksGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      // step 2 - declaring the components
      val broadcast = builder.add(Broadcast[Int](2))

      // step 3 - tying up the components
      input ~> broadcast
      broadcast.out(0) ~> firstSink
      broadcast.out(1) ~> secondSink

      // step 4
      ClosedShape
    }
  )
}