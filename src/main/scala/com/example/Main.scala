package com.example

import distage.{Tag, _}
import zio._
import zio.console._

object Main extends App {
  def run(args: List[String]) = {
    def provideHas[R: HasConstructor, A: Tag](fn: R => A): ProviderMagnet[A] =
      HasConstructor[R].map(fn)

    val program =
      for {
        _ <- putStrLn("distage")
      } yield ExitCode.success

    val definition = new ModuleDef {
      make[Console.Service].fromHas(Console.live)
      make[UIO[ExitCode]].from(provideHas(program.provide))
    }

    Injector()
      .produceGetF[Task, UIO[ExitCode]](definition)
      .useEffect
      .catchAll(e => UIO(println(s"failed to start $e")).as(ExitCode.failure))
  }
}
