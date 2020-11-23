package com.example

import capture.Capture
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.zio.instances._
import com.sksamuel.elastic4s.{ElasticClient, ElasticDsl}
import distage.Id
import zio.macros.accessible
import zio.{Has, IO, Ref, ZIO}

@accessible
trait SearchClient {
  def add(items: String): IO[Capture[SearchErr], Unit]

  def search(item: String): IO[Capture[SearchErr], Boolean]
}

object SearchClient {
  def make(index: String@Id("index"), field: String@Id("field")) =
    for {
      client <- ZIO.service[ElasticClient]
    } yield new SearchClient {
      def search(item: String) =
        for {
          resp <- client.execute(
            ElasticDsl.search(index).query(item)
          ).mapError(SearchErr.throwable("elastic search"))
          res <- IO.fromEither(resp.toEither)
            .bimap(
              e => SearchErr.message(s"RequestFailure ${e.reason}"),
              _.nonEmpty
            )
        } yield res

      def add(items: String) = for {
        _ <- client.execute(
          indexInto(index).fields(field -> items)
        ).mapError(SearchErr.throwable("indexInto"))
      } yield ()
    }

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
