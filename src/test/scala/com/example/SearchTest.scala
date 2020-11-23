package com.example

import com.example.fixture.Rnd.rnd
import com.example.fixture.{ElasticDocker, ElasticDockerSvc}
import distage._
import izumi.distage.model.definition.ModuleDef
import izumi.distage.testkit.TestConfig
import izumi.distage.testkit.scalatest.DistageBIOEnvSpecScalatest
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest._
import sttp.model.Uri._
import zio.Schedule.{elapsed, exponential}
import zio._
import zio.duration._

abstract class SearchTest extends DistageBIOEnvSpecScalatest[ZIO] with OptionValues with EitherValues with TypeCheckedTripleEquals {
  val retryPolicy = (exponential(10.milliseconds) >>> elapsed).whileOutput(_ < 1.minute)
  "Demo app" should {
    "add and find foo" in {
      (for {
        _ <- Logic.>.add("foo bar bazz")
        matched <- Logic.>.check("foo")
        _ <- IO {
          assert(matched)
        }
      } yield ()).retry(retryPolicy)
    }
    "not find some random" in {
      (for {
        rndItem <- rnd[String]
        notMatched <- Logic.>.check(rndItem)
        _ <- IO {
          assert(!notMatched)
        }
      } yield ()).retry(retryPolicy)
    }
    "error on 42" in {
      for {
        error42 <- Logic.>.check("42").either
        str = error42.left.value continue new LogicErr[String] with SearchErr[String] {
          def no42(message: String): String = "case 42"

          def throwable(message: String)(e: Throwable) = ???

          def message(message: String) = ???
        }
        _ = assert(str === "case 42")
      } yield ()
    }
  }
}

final class DummySearchTest extends SearchTest {
  override def config: TestConfig = super.config.copy(
    moduleOverrides = new ModuleDef {
      make[SearchClient]
        .fromEffect(Ref.make(Set.empty[String]).map(SearchClient.dummy))
    }
  )
}

final class DockerSearchTest extends SearchTest {
  override def config: TestConfig = super.config.copy(
    moduleOverrides = new ModuleDef {
      make[AppConf].fromEffect { (service: ElasticDockerSvc, index: String@Id("index"), field: String@Id("field")) =>
        for {
          url <- Task(uri"http://${service.es.hostV4}:${service.es.port}")
        } yield AppConf(url.toString)
      }
    },
    memoizationRoots = Set(
      DIKey.get[ElasticDocker.Container],
    ),
  )
}
