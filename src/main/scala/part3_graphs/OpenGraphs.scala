package part3_graphs

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, FlowShape, SinkShape, SourceShape}
import akka.stream.scaladsl.{Balance, Broadcast, Concat, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip}

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
  val incrementer = Flow[Int].map(x => x + 1)
  val multiplier = Flow[Int].map(x => x * 10)

  // step 1
  val flowGraph = Flow.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      // everything operates on SHAPES

      // step 2: define auxiliary SHAPES
      val incrementerShape = builder.add(incrementer)
      val multiplierShape = builder.add(multiplier)

      // step 3: connect the SHAPES
      incrementerShape ~> multiplierShape

      // step 4
      FlowShape(incrementerShape.in, multiplierShape.out) // SHAPE
    } // static graph
  ) // component

  firstSource.via(flowGraph).to(Sink.foreach(println)).run()

  /**
   * Exercise: flow from a sink and a source
   */
  def fromSinkAndSource[A, B](sink: Sink[A, _], source: Source[B, _]): Flow[A, B, _] = {
    // step 1
    Flow.fromGraph(
      GraphDSL.create() { implicit builder =>
        // step 2: declare the SHAPES
        val sourceShape = builder.add(source)
        val sinkShape = builder.add(sink)
        // step 3
        // step 4 - return the shape
        FlowShape(sinkShape.in, sourceShape.out)
      }
    )
  }

  val f = Flow.fromSinkAndSourceCoupled(Sink.foreach[String](println), Source(1 to 10))
}
