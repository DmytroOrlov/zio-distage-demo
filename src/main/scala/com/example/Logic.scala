package com.example

import capture.Capture
import capture.Capture.Constructors
import zio._
import zio.macros.accessible

@accessible
trait Logic {
  def check(item: String): IO[Capture[LogicErr with SearchErr], Boolean]
}

object Logic {
  val make = for {
    env <- ZIO.environment[Has[SearchClient]]
  } yield new Logic {
    def check(item: String) =
      (for {
        _ <- IO.fail(LogicErr.no42(s"item=$item"))
          .when(item.contains("42"))
        res <- SearchClient.>.search(item)
      } yield res)
        .provide(env)
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
