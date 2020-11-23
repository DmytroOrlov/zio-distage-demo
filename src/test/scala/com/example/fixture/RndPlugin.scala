package com.example.fixture

import distage.plugins.PluginDef
import izumi.functional.bio.{BIO, F}
import org.scalacheck.Gen.Parameters
import org.scalacheck.{Arbitrary, Prop}
import zio._

object RndPlugin extends PluginDef {
  make[Rnd[IO]].from[Rnd.Impl[IO]]
}

trait Rnd[F[_, _]] {
  def apply[A: Arbitrary]: F[Nothing, A]
}

object Rnd {

  object rnd extends Rnd[ZIO[Has[Rnd[IO]], ?, ?]] {
    override def apply[A: Arbitrary]: URIO[Has[Rnd[IO]], A] = ZIO.accessM(_.get.apply[A])
  }

  final class Impl[F[+_, +_] : BIO] extends Rnd[F] {
    override def apply[A: Arbitrary]: F[Nothing, A] = {
      F.sync {
        val (p, s) = Prop.startSeed(Parameters.default)
        Arbitrary.arbitrary[A].pureApply(p, s)
      }
    }
  }

}
