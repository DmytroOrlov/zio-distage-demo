package com

import sttp.tapir.Codec.PlainCodec
import sttp.tapir.generic.Configuration

package object example {
  implicit val tapirSnakeCaseConfig =
    Configuration.default.withSnakeCaseMemberNames

  implicit val itemCodec: PlainCodec[Item] =
    implicitly[PlainCodec[String]].map(Item)(_.value)
}
