import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.4.1"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven { url = uri("https://jitpack.io") }
}

val spoutVersion = project.providers.gradleProperty("spoutVersion").get()
dependencies {
    compileOnly("com.github.ModernSpout:Spout-Paper-server:$spoutVersion")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

val serverDir: File = projectDir.resolve("run")
val pluginDir: File = serverDir.resolve("plugins")

tasks {
    build {
        dependsOn(shadowJar)
    }

    val spoutJarURL =
        "https://github.com/ModernSpout/Spout-Paper-server/releases/download/$spoutVersion/spout-$spoutVersion.jar"
    register("downloadServer") {
        group = "spout"; notCompatibleWithConfigurationCache(""); doFirst {
        serverDir.mkdirs(); pluginDir.mkdirs()
        URI(spoutJarURL).toURL().openStream().use {
            Files.copy(it, serverDir.resolve("server.jar").toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
    }
    register("runServer", JavaExec::class) {
        group = "spout"; notCompatibleWithConfigurationCache("")
        dependsOn("shadowJar")
        if (!serverDir.resolve("server.jar").exists()) dependsOn("downloadServer")
        doFirst {
            pluginDir.resolve("${project.name}.jar").delete()
            Files.copy(
                layout.buildDirectory.file("libs/${project.name}-${version}.jar").get().asFile.toPath(),
                pluginDir.resolve("${project.name}.jar").toPath()
            )
        }
        classpath = files(serverDir.resolve("server.jar"))
        workingDir = serverDir; jvmArgs = listOf("-Dcom.mojang.eula.agree=true", "-Dspout.server.paper.enabled=true")
        args = listOf("--nogui"); standardInput = System.`in`
    }

    processResources {
        val props = mapOf(
            "version" to version,
            "description" to project.description,
            "apiVersion" to "\"${project.providers.gradleProperty("apiVersion").get()}\"",
        )
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
}
