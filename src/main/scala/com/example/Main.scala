package com.example

import buildinfo.BuildInfo.version
import distage.{Tag, _}
import org.http4s.HttpRoutes
import org.http4s.server.Router
import sttp.tapir.docs.openapi._
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import zio._
import zio.interop.catz._

object Main extends App {
  def run(args: List[String]) = {
    def provideHas[R: HasConstructor, A: Tag](fn: R => A): ProviderMagnet[A] =
      HasConstructor[R].map(fn)

    val route = for {
      implicit0(rts: Runtime[Any]) <- ZIO.runtime[Any]
      add <- Endpoints.>.add
      check <- Endpoints.>.check
      docs = Seq(add, check).toOpenAPI("demo", version)
      route = Router[Task](
        "/" -> new SwaggerHttp4s(docs.toYaml).routes
      )
    } yield route

    val program = HttpServer.>.bindHttp *> IO.never

    val definition = new ModuleDef {
      make[Endpoints].fromValue(Endpoints.make)
      many[HttpRoutes[Task]]
        .addHas(route)
      make[HttpServer].fromResource(HttpServer.make _)
      make[UIO[ExitCode]].from(provideHas(program.provide))
    }

    Injector()
      .produceGetF[Task, UIO[ExitCode]](definition)
      .useEffect
      .catchAll(e => UIO(println(s"failed to start $e")).as(ExitCode.failure))
  }
}
