package part3_graphs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.scaladsl.{Balance, Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip}

import scala.concurrent.duration._

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
  )//.run()

  val firstSink = Sink.foreach[Int](x => println(s"First Sink: $x"))
  val secondSink = Sink.foreach[Int](x => println(s"Second Sink: $x"))

  // step 1
  val sourceToTwoSinksGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      // step 2 - declaring the components
      val broadcast = builder.add(Broadcast[Int](2))

      // step 3 - tying up the components
      input ~> broadcast ~> firstSink
               broadcast ~> secondSink
//      broadcast.out(0) ~> firstSink
//      broadcast.out(1) ~> secondSink

      // step 4
      ClosedShape
    }
  )

  /**
   * Exercise 2
   */
  val fastSourceAttempt = Source(1 to 1000)
  val slowSourceAttempt = Source(1 to 1000).throttle(2, 1 second)

  // step 1
  RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      // step 2 - declaring the components
      val merge = builder.add(Merge[Int](2))
      val balance = builder.add(Balance[Int](2))

      // step 3 - tying up the components
      fastSourceAttempt ~> merge
      slowSourceAttempt ~> merge
      balance ~> firstSink
      balance ~> secondSink
      merge ~> balance

      // step 4
      ClosedShape
    }
  )//.run()

  val fastSource = input.throttle(5, 1 second)
  val slowSource = input.throttle(2, 1 second)

  val sink1 = Sink.foreach[Int](x => println(s"Sink 1: $x"))
  val sink2 = Sink.foreach[Int](x => println(s"Sink 2: $x"))

  val balanceGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      // step 2 -- declare components
      val merge = builder.add(Merge[Int](2))
      val balance = builder.add(Balance[Int](2))

      // step 3 -- tie them up
      fastSource ~> merge ~> balance ~> sink1
      slowSource ~> merge ;  balance ~> sink2

      // step 4
      ClosedShape
    }
  )
  balanceGraph.run()
}
