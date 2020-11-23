package com.example

import com.example.fixture.Rnd.rnd
import izumi.distage.testkit.scalatest.DistageBIOEnvSpecScalatest
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest._
import zio._

class SearchTest extends DistageBIOEnvSpecScalatest[ZIO] with OptionValues with EitherValues with TypeCheckedTripleEquals {
  "Demo app" should {
    //        "add and find foo" in {
    "not find some random" in {
      for {
        rndItem <- rnd[String]
        notMatched <- Logic.>.check(rndItem)
        _ = assert(!notMatched)
      } yield ()
    }
    "error on 42" in {
      for {
        error42 <- Logic.>.check("42").either
        str = error42.left.value continue new LogicErr[String] {
          def no42(message: String): String = "case 42"
        }
        _ = assert(str === "case 42")
      } yield ()
    }
  }
}
