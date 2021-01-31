package part4_techniques

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.util.Timeout

import java.util.Date
import scala.concurrent.Future

object IntegratingWithExternalServices extends App {

  implicit val system: ActorSystem = ActorSystem("IntegratingWithExternalServices")
  implicit val materialize: ActorMaterializer = ActorMaterializer()

  import system.dispatcher // not recommended in practice for mapAsync
  implicit val dispatcher = system.dispatchers.lookup("dedicated-dispatcher")

  def genericExtService[A, B](element: A): Future[B] = ???

  // example: simplified PagerDuty
  case class PagerEvent(application: String, description: String, date: Date)

  val eventSource = Source(List(
    PagerEvent("AkkaInfra", "Infrastructure broke", new Date),
    PagerEvent("FastDataPipeline", "Illegal elements in the data pipeline", new Date),
    PagerEvent("AkkaInfra", "A service stopped responding", new Date),
    PagerEvent("SuperFrontend", "a button doesn't work", new Date)
  ))

  object PagerService {
    private val engineers = List("Daniel", "John", "Lady Gaga")
    private val emails = Map(
      "Daniel" -> "daniel@rockthejvm.com",
      "John" -> "john@rockthejvm.com",
      "Lady Gaga" -> "ladygaga@rtjvm.com"
    )

    def processEvent(pagerEvent: PagerEvent) = Future {
      val engineerIndex = (pagerEvent.date.toInstant.getEpochSecond / (24 * 3600)) % engineers.length
      val engineer = engineers(engineerIndex.toInt)
      val engineerEmail = emails(engineer)

      // page the engineer
      println(s"Sending engineer $engineerEmail a high priority notification: $pagerEvent")

      // return the email that was paged
      engineerEmail
    }
  }

  val infraEvents = eventSource.filter((_.application == "AkkaInfra"))
  val pagedEngineerEmails = infraEvents.mapAsync(parallelism = 4)(event => PagerService.processEvent(event))
  // guarantees the relative order of events
  val pagedEmailsSink = Sink.foreach[String](email => println(s"Successfully sent notification to $email"))

  //  pagedEngineerEmails.to(pagedEmailsSink).run()

  class PagerActor extends Actor with ActorLogging {
    private val engineers = List("Daniel", "John", "Lady Gaga")
    private val emails = Map(
      "Daniel" -> "daniel@rockthejvm.com",
      "John" -> "john@rockthejvm.com",
      "Lady Gaga" -> "ladygaga@rtjvm.com"
    )

    def processEvent(pagerEvent: PagerEvent) = Future {
      val engineerIndex = (pagerEvent.date.toInstant.getEpochSecond / (24 * 3600)) % engineers.length
      val engineer = engineers(engineerIndex.toInt)
      val engineerEmail = emails(engineer)

      // page the engineer
      println(s"Sending engineer $engineerEmail a high priority notification: $pagerEvent")

      // return the email that was paged
      engineerEmail
    }

    override def receive: Receive = {
      case pagerEvent: PagerEvent =>
        sender() ! processEvent(pagerEvent)
    }
  }

  import akka.pattern.ask
  import scala.concurrent.duration._

  implicit val timeout = Timeout(3 seconds)
  val pagerActor = system.actorOf(Props[PagerActor], "pagerActor")
  val alternativePagedEngineerEmails = infraEvents.mapAsync(parallelism = 4)(event => (pagerActor ? event).mapTo[String])
  alternativePagedEngineerEmails.to(pagedEmailsSink).run()

  // do not confuse
}
