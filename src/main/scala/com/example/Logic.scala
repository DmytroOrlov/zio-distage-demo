package com.example

import capture.Capture
import capture.Capture.Constructors
import zio._
import zio.macros.accessible

@accessible
trait Logic {
  def check(item: String): IO[Capture[LogicErr], Boolean]
}

object Logic {
  val mock = new Logic {
    def check(item: String) =
      for {
        _ <- IO.fail(LogicErr.no42(s"item=$item"))
          .when(item.contains("42"))
      } yield false
  }
}

trait LogicErr[+A] {
  def no42(message: String): A
}

object LogicErr extends Constructors[LogicErr] {
  def no42(message: String) =
    Capture[LogicErr](_.no42(message))

  trait AsFailureResp extends LogicErr[FailureResp] {
    def no42(message: String) = FailureResp(s"no42: $message")
  }

}
