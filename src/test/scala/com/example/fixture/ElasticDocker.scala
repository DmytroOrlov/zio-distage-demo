package com.example.fixture

import izumi.distage.docker.ContainerDef
import izumi.distage.docker.Docker.{ContainerConfig, DockerPort}

object ElasticDocker extends ContainerDef {
  val primaryPort: DockerPort = DockerPort.TCP(9200)

  override def config: Config = {
    ContainerConfig(
      image = "docker.elastic.co/elasticsearch/elasticsearch:7.10.0",
      ports = Seq(primaryPort),
      env = Map("discovery.type" -> "single-node"),
    )
  }
}

import izumi.distage.docker.Docker.AvailablePort
import izumi.distage.docker.modules.DockerSupportModule
import izumi.distage.model.definition.Id
import izumi.distage.model.definition.StandardAxis.Env
import izumi.distage.plugins.PluginDef
import zio.Task

class ElasticDockerSvc(val es: AvailablePort@Id("es"))

object ElasticPlugin extends DockerSupportModule[Task] with PluginDef {
  make[ElasticDocker.Container].fromResource {
    ElasticDocker.make[Task]
  }

  make[AvailablePort].named("es").tagged(Env.Test).from {
    dn: ElasticDocker.Container =>
      dn.availablePorts.first(ElasticDocker.primaryPort)
  }

  make[ElasticDockerSvc]
}
