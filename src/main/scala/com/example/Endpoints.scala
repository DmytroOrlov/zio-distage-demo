package com.example

import io.circe.generic.auto._
import sttp.tapir._
import sttp.tapir.json.circe._
import zio._
import zio.macros.accessible

@accessible
trait Endpoints {
  def check: UIO[Endpoint[Item, FailureResp, CheckResp, Nothing]]

  def add: UIO[Endpoint[AddRequest, FailureResp, AddResp, Nothing]]
}

case class Item(value: String) extends AnyVal

case class CheckResp(matched: Boolean)

case class FailureResp(error: String)

case class AddResp(ok: String)

case class AddRequest(items: String)

object Endpoints {
  val make = new Endpoints {
    val check = IO.succeed {
      endpoint.get
        .in("check")
        .in(path[Item]("check"))
        .out(jsonBody[CheckResp])
        .errorOut(jsonBody[FailureResp])
    }
    val add = IO.succeed {
      endpoint.post
        .in("add")
        .in(jsonBody[AddRequest])
        .out(jsonBody[AddResp])
        .errorOut(jsonBody[FailureResp])
    }
  }
}
