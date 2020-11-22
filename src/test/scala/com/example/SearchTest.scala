package com.example

import izumi.distage.testkit.scalatest.DistageBIOEnvSpecScalatest
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest._
import zio._

class SearchTest extends DistageBIOEnvSpecScalatest[ZIO] with OptionValues with EitherValues with TypeCheckedTripleEquals {
  "Demo app" should {
    //        "add and find foo" in {
    "not find some random" in {
      for {
        _ <- zio.IO.unit
        notMatched <- Logic.>.check("asdf")
        _ = assert(!notMatched)
      } yield ()
    }
    //        "error on 42" in {

  }
}
