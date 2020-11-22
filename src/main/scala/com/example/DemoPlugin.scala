package com.example

import distage.{HasConstructor, ProviderMagnet}
import izumi.distage.effect.modules.ZIODIEffectModule
import izumi.distage.plugins.PluginDef
import org.http4s.HttpRoutes
import zio.{ExitCode, Tag, Task, UIO}

object DemoPlugin extends PluginDef with ZIODIEffectModule {
  def provideHas[R: HasConstructor, A: Tag](fn: R => A): ProviderMagnet[A] =
    HasConstructor[R].map(fn)

  make[Logic].fromValue(Logic.mock)
  make[Endpoints].fromValue(Endpoints.make)
  many[HttpRoutes[Task]]
    .addHas(Main.logicRoutes)
  make[HttpServer].fromResource(HttpServer.make _)
  make[UIO[ExitCode]].from(provideHas(Main.program.provide))
}
