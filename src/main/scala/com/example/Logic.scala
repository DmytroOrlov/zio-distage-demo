package com.example

import zio._
import zio.macros.accessible

@accessible
trait Logic {
  def check(item: String): IO[LogicErr, Boolean]
}

object Logic {
  val mock = new Logic {
    def check(item: String) =
      IO.succeed(false)
  }
}

sealed trait LogicErr
