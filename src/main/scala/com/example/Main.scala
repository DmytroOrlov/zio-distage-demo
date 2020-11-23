package com.example

import buildinfo.BuildInfo.version
import cats.syntax.semigroupk._
import distage._
import izumi.distage.plugins.PluginConfig
import izumi.distage.plugins.load.PluginLoader
import org.http4s.HttpRoutes
import org.http4s.server.Router
import sttp.tapir.docs.openapi._
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.server.http4s._
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import zio._
import zio.interop.catz._

object Main extends App {
  val logicRoutes = for {
    implicit0(rts: Runtime[Any]) <- ZIO.runtime[Any]
    env <- ZIO.environment[Has[Logic]]
    add <- Endpoints.>.add
    check <- Endpoints.>.check
    docs = Seq(add, check).toOpenAPI("demo", version)
    router = Router[Task](
      "/" -> ((check.toRoutes { req =>
        Logic.>.check(req.value)
          .bimap(
            _ continue new SearchErr.AsFailureResp with LogicErr.AsFailureResp {},
            CheckResp.apply)
          .either
          .provide(env)
      }: HttpRoutes[Task]) <+> add.toRoutes { req =>
        Logic.>.add(req.items)
          .bimap(
            _ continue new LogicErr.AsFailureResp with SearchErr.AsFailureResp {},
            _ => AddResp("ok"))
          .either.provide(env)
      } <+>
        new SwaggerHttp4s(docs.toYaml).routes)
    )
  } yield router

  val program = HttpServer.>.bindHttp *> IO.never

  def run(args: List[String]) = {
    val pluginConfig = PluginConfig.cached(
      packagesEnabled = Seq(
        "com.example",
      )
    )
    val appModules = PluginLoader().load(pluginConfig)

    Injector()
      .produceGetF[Task, UIO[ExitCode]](appModules.merge)
      .useEffect
      .catchAll(e => UIO(println(s"failed to start $e")).as(ExitCode.failure))
  }
}
