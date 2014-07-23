package com.dataxu

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import spray.can.Http
import spray.can.server.Stats
import spray.http.HttpMethods._
import spray.http.MediaTypes._
import spray.http._
import spray.util._

class BidConfirmationService extends Actor with ActorLogging {
  implicit val timeout: Timeout = 1.second // for the actor 'asks'
  import context.dispatcher // ExecutionContext for the futures and scheduler

  def receive = {
    // when a new connection comes in we register ourselves as the connection handler
    case _: Http.Connected => sender ! Http.Register(self)

    case HttpRequest(GET, Uri.Path("/"), _, _, _) =>
      sender ! HttpResponse(entity = "Mock!")

    case HttpRequest(GET, Uri.Path("/x/bcs0"), _, _, _) =>
      sender ! HttpResponse(entity = "OK")

    case HttpRequest(GET, Uri.Path("/stats"), _, _, _) =>
      val client = sender
      context.actorFor("/user/IO-HTTP/listener-0") ? Http.GetStats onSuccess {
        case x: Stats => client ! statsPresentation(x)
      }

    case _: HttpRequest => sender ! HttpResponse(status = 404, entity = "Unknown resource!")
  }

  def statsPresentation(s: Stats) = HttpResponse(
    entity = HttpEntity(`text/html`,
      <html>
        <body>
          <h1>HttpServer Stats</h1>
          <table>
            <tr><td>uptime:</td><td>{s.uptime.formatHMS}</td></tr>
            <tr><td>totalRequests:</td><td>{s.totalRequests}</td></tr>
            <tr><td>openRequests:</td><td>{s.openRequests}</td></tr>
            <tr><td>maxOpenRequests:</td><td>{s.maxOpenRequests}</td></tr>
            <tr><td>totalConnections:</td><td>{s.totalConnections}</td></tr>
            <tr><td>openConnections:</td><td>{s.openConnections}</td></tr>
            <tr><td>maxOpenConnections:</td><td>{s.maxOpenConnections}</td></tr>
            <tr><td>requestTimeouts:</td><td>{s.requestTimeouts}</td></tr>
          </table>
        </body>
      </html>.toString()
    )
  )
}