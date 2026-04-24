import java.net.URI
import java.nio.file.Files

plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.4.1"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    compileOnly("com.github.ModernSpout:Spout-Paper-server:ecd8613d52")
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

    register("downloadServer") {
        group = "spout"
        doFirst {
            serverDir.mkdirs()
            pluginDir.mkdirs()
            URI("https://github.com/ModernSpout/Spout-Paper-server/releases/download/1.21.11-R0.1/spout-paperclip-1.21.11-R0.1.jar").toURL().openStream().use {
                Files.copy(it, serverDir.resolve("server.jar").toPath())
            }
        }
    }

    register("runServer", JavaExec::class) {
        notCompatibleWithConfigurationCache("Uses non-serializable Gradle script references")
        group = "spout"
        dependsOn("shadowJar")
        if (!serverDir.resolve("server.jar").exists()) {
            dependsOn("downloadServer")
        }
        doFirst {
            pluginDir.resolve("Quark.jar").delete()
            Files.copy(
                layout.buildDirectory.file("libs/Quark-plugin-${version}.jar").get().asFile.toPath(),
                pluginDir.resolve("Quark.jar").toPath()
            )
        }
        classpath = files(serverDir.resolve("server.jar"))
        workingDir = serverDir
        jvmArgs = listOf("-Dcom.mojang.eula.agree=true")
        args = listOf("--nogui")
        standardInput = System.`in`
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
