import xyz.jpenilla.resourcefactory.bukkit.BukkitPluginYaml

plugins {
  `java-library`
  id("io.papermc.paperweight.userdev") version "1.5.13"
  id("xyz.jpenilla.run-paper") version "2.2.3"
  id("xyz.jpenilla.resource-factory-bukkit-convention") version "1.1.1"
}

group = "de.derioo.server"
version = "0.0.0"
description = "Backend Server Mod Wrapper"

java {
  toolchain.languageVersion = JavaLanguageVersion.of(17)
}

dependencies {
  paperweight.paperDevBundle("1.20.4-R0.1-SNAPSHOT")
}

tasks {
  assemble {
    dependsOn(reobfJar)
  }

  compileJava {
    options.encoding = Charsets.UTF_8.name()

    options.release = 17
  }
  javadoc {
    options.encoding = Charsets.UTF_8.name()
  }

}

bukkitPluginYaml {
  main = "de.derioo.simpleChals.server.ServerCore"
  load = BukkitPluginYaml.PluginLoadOrder.STARTUP
  authors.add("Dario")
  apiVersion = "1.20"
}
