package com.example.fixture

import distage.Mode
import distage.plugins.PluginDef
import izumi.distage.docker.ContainerDef
import izumi.distage.docker.Docker.{AvailablePort, ContainerConfig, DockerPort}
import izumi.distage.docker.modules.DockerSupportModule
import izumi.distage.model.definition.Id
import zio.Task

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

class ElasticDockerSvc(val es: AvailablePort@Id("es"))

object ElasticPlugin extends PluginDef {
  include(DockerSupportModule[Task])

  make[ElasticDocker.Container].fromResource {
    ElasticDocker.make[Task]
  }

  make[AvailablePort].named("es").tagged(Mode.Test).from {
    dn: ElasticDocker.Container =>
      dn.availablePorts.first(ElasticDocker.primaryPort)
  }

  make[ElasticDockerSvc]
}
