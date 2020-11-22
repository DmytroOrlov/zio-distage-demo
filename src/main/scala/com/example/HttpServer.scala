package com.example

import cats.syntax.semigroupk._
import distage.Id
import org.http4s.HttpRoutes
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.kleisli._
import zio._
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.macros.accessible

import scala.concurrent.ExecutionContext

@accessible
trait HttpServer {
  def bindHttp: UIO[Server[Task]]
}

object HttpServer {
  def make(routes: Set[HttpRoutes[Task]] /*, ec: ExecutionContext@Id("zio.cpu")*/) =
    for {
      implicit0(rts: Runtime[Any]) <- ZIO.runtime[Any].toManaged_
      route = routes.reduce(_ <+> _)
      srv <- BlazeServerBuilder[Task](ExecutionContext.Implicits.global)
        .withHttpApp(route.orNotFound)
        .bindHttp()
        .resource
        .toManaged
    } yield new HttpServer {
      val bindHttp = IO.succeed(srv)
    }
}
