package com.example

import com.sksamuel.elastic4s.sttp.SttpRequestHttpClient
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties}
import com.typesafe.config.ConfigFactory
import distage.config.ConfigModuleDef
import distage.{HasConstructor, ProviderMagnet}
import izumi.distage.config.AppConfigModule
import izumi.distage.effect.modules.ZIODIEffectModule
import izumi.distage.plugins.PluginDef
import org.http4s.HttpRoutes
import zio._

case class AppConf(url: String)

object DemoPlugin extends PluginDef with ConfigModuleDef with ZIODIEffectModule {
  def provideHas[R: HasConstructor, A: Tag](fn: R => A): ProviderMagnet[A] =
    HasConstructor[R].map(fn)

  def makeEsClient(cfg: AppConf) =
    for {
      c <- IO {
        SttpRequestHttpClient(ElasticProperties(cfg.url).endpoints.head)
      }.toManaged(c => UIO(c.close()))
    } yield ElasticClient(c)

  include(AppConfigModule(ConfigFactory.defaultApplication()))

  make[SearchClient]
    .fromHas(SearchClient.make _)

  makeConfig[AppConf]("app")
  make[String].named("index").fromValue("items")
  make[String].named("field").fromValue("item")
  make[ElasticClient].fromResource(makeEsClient _)
  make[Logic].fromHas(Logic.make)
  make[Endpoints].fromValue(Endpoints.make)
  many[HttpRoutes[Task]]
    .addHas(Main.logicRoutes)
  make[HttpServer].fromResource(HttpServer.make _)
  make[UIO[Unit]].from(provideHas(Main.program.provide))
}
