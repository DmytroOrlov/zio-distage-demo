ThisBuild / organization := "com.example"
ThisBuild / version := "1.0-SNAPSHOT"
ThisBuild / scalacOptions ++= Seq(
  "-Ymacro-annotations",
)

val V = new {
  val zioInteropCats = "2.2.0.1"
  val zio = "1.0.3"
  val distage = "0.10.19"
  val tapir = "0.16.16"
  val sttp = "2.2.9"
  val elastic4s = "7.9.1"

  val scalacheck = "1.15.1"

  val betterMonadicFor = "0.3.1"
  val kindProjector = "0.11.1"

  val silencer = "1.7.1"
}

val Deps = new {
  val zioInteropCats = "dev.zio" %% "zio-interop-cats" % V.zioInteropCats
  val zio = "dev.zio" %% "zio" % V.zio
  val distageFramework = "io.7mind.izumi" %% "distage-framework" % V.distage
  val distageFrameworkDocker = "io.7mind.izumi" %% "distage-framework-docker" % V.distage
  val distageTestkitScalatest = "io.7mind.izumi" %% "distage-testkit-scalatest" % V.distage
  val logstageAdapterSlf4J = "io.7mind.izumi" %% "logstage-adapter-slf4j" % V.distage

  val tapirJsonCirce = "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % V.tapir
  val tapirHttp4sServer = "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % V.tapir
  val tapirOpenapiCirceYaml = "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % V.tapir
  val tapirOpenapiDocs = "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % V.tapir
  val tapirSwaggerUiHttp4s = "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-http4s" % V.tapir

  val sttpClientCirce = "com.softwaremill.sttp.client" %% "circe" % V.sttp
  val asyncHttpClientBackendZio = "com.softwaremill.sttp.client" %% "async-http-client-backend-zio" % V.sttp

  val elastic4sClientSttp = "com.sksamuel.elastic4s" %% "elastic4s-client-sttp" % V.elastic4s
  val elastic4sEffectZio = "com.sksamuel.elastic4s" %% "elastic4s-effect-zio" % V.elastic4s

  val scalacheck = "org.scalacheck" %% "scalacheck" % V.scalacheck

  val betterMonadicFor = "com.olegpy" %% "better-monadic-for" % V.betterMonadicFor
  val kindProjector = "org.typelevel" %% "kind-projector" % V.kindProjector cross CrossVersion.full
}

val commonSettings = Seq(
  scalaVersion := "2.13.4",
)

lazy val `zio-distage-demo` = (project in file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      Deps.zio,
      Deps.zioInteropCats,
      Deps.logstageAdapterSlf4J,
      Deps.distageFramework,

      Deps.distageFrameworkDocker % Test,
      Deps.distageTestkitScalatest % Test,
      Deps.scalacheck % Test,

      Deps.sttpClientCirce,
      Deps.asyncHttpClientBackendZio,

      Deps.tapirJsonCirce,
      Deps.tapirHttp4sServer,
      Deps.tapirOpenapiCirceYaml,
      Deps.tapirOpenapiDocs,
      Deps.tapirSwaggerUiHttp4s,

      Deps.elastic4sClientSttp,
      Deps.elastic4sEffectZio,
    ),
    addCompilerPlugin(Deps.betterMonadicFor),
    addCompilerPlugin(Deps.kindProjector),
  )
  .dependsOn(macros)

lazy val macros = project
  .disablePlugins(RevolverPlugin)
  .settings(
    scalaVersion := "2.13.3",
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % V.silencer cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % V.silencer % Provided cross CrossVersion.full,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided",
      "dev.zio" %% "zio-test-sbt" % V.zio % Test,
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    scalacOptions += "-language:experimental.macros",
  )
