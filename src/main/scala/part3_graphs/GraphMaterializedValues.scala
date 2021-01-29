package part3_graphs

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, SinkShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Sink, Source}

object GraphMaterializedValues extends App  {

  implicit val system = ActorSystem("GraphMaterializedValues")
  implicit val materialize: ActorMaterializer = ActorMaterializer()

  val wordSource = Source(List("Akka", "is", "awesome", "rock", "the", "jvm"))
  val printer = Sink.foreach[String](println)
  val counter = Sink.fold[Int, String](0)((count, _) => count + 1)

  /*
    A composite component (sink)
    - prints out all strings which are lowercase
    - COUNTS the strings that are short (< 5 chars)
   */

  // step 1
  val complexWordSink = Sink.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      // step 2 - SHAPES
      val broadcast = builder.add(Broadcast[String](2))
      val lowercaseFilter = builder.add(Flow[String].filter(word => word == word.toLowerCase))
      val shortStringFilter = builder.add(Flow[String].filter(_.length < 5))

      // step 3 - connections
      broadcast ~> lowercaseFilter ~> printer
      broadcast ~> shortStringFilter ~> counter

      // step 4 - the shape
      SinkShape(broadcast.in)
    }
  )
}
