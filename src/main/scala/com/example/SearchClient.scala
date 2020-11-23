package com.example

import capture.Capture
import zio.macros.accessible
import zio.{IO, Ref}

@accessible
trait SearchClient {
  def add(items: String): IO[Capture[SearchErr], Unit]

  def search(item: String): IO[Capture[SearchErr], Boolean]
}

object SearchClient {
  def dummy(store: Ref[Set[String]]): SearchClient =
    new SearchClient {
      def add(items: String) = for {
        _ <- store.getAndUpdate(_ + items)
      } yield ()

      def search(item: String) =
        for {
          items <- store.get
        } yield items.exists(_.contains(item))
    }
}

import capture.Capture
import capture.Capture.Constructors

trait SearchErr[+A] {
  def throwable(message: String)(e: Throwable): A

  def message(message: String): A
}

object SearchErr extends Constructors[SearchErr] {
  def throwable(message: String)(e: Throwable) =
    Capture[SearchErr](_.throwable(message)(e))

  def message(message: String) =
    Capture[SearchErr](_.message(message))

  trait AsFailureResp extends SearchErr[FailureResp] {
    def throwable(message: String)(e: Throwable) = FailureResp(s"$message: ${e.getMessage}")

    def message(message: String) = FailureResp(message)
  }

}
