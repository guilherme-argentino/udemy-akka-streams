package part3_graphs

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, SinkShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Sink, Source}

import scala.util.{Failure, Success}

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
    GraphDSL.create(counter) { implicit builder => counterShape =>
      import GraphDSL.Implicits._

      // step 2 - SHAPES
      val broadcast = builder.add(Broadcast[String](2))
      val lowercaseFilter = builder.add(Flow[String].filter(word => word == word.toLowerCase))
      val shortStringFilter = builder.add(Flow[String].filter(_.length < 5))

      // step 3 - connections
      broadcast ~> lowercaseFilter ~> printer
      broadcast ~> shortStringFilter ~> counterShape

      // step 4 - the shape
      SinkShape(broadcast.in)
    }
  )

  import system.dispatcher
  val shortStringsCountFuture = wordSource.toMat(complexWordSink)(Keep.right).run()
  shortStringsCountFuture.onComplete {
    case Success(count) => println(s"The total number of short strings is: $count")
    case Failure(exception) => println(s"The count of short strings failed: $exception")
  }
}
