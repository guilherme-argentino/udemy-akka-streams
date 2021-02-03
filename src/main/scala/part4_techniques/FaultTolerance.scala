package part4_techniques

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

object FaultTolerance extends App {

  implicit val system = ActorSystem("FaultTolerance")
  implicit val materializer = ActorMaterializer()

  // 1 - logging
  val faultySource = Source(1 to 10).map(e => if(e == 6) throw new RuntimeException else e)
  faultySource.log("trackingElements").to(Sink.ignore)
  //    .run()

  // 2 - gracefully terminating a stream
  faultySource.recover {
    case _: RuntimeException => Int.MinValue
  } .log("gracefulSource")
    .to(Sink.ignore)
    .run()


}
