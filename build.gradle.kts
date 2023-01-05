plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.8.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("xyz.jpenilla.run-paper") version "2.0.1" // Adds runServer and runMojangMappedServer tasks for testing
    id("net.minecrell.plugin-yml.bukkit") version "0.5.2" // Generates plugin.yml
}

group = "com.boy0000.ModernLightApi"
version = "0.1"


allprojects {
    apply(plugin = "java")

    repositories {
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.19.3-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

tasks {

    prepareKotlinBuildScriptModel {

    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        filteringCharset = Charsets.UTF_8.name()
    }

    runServer {
        minecraftVersion("1.19.3")
    }

    shadowJar {
        archiveFileName.set("ModernLightApi.jar")
        //archiveFile.get().asFile.copyTo(layout.projectDirectory.file("run/plugins/ModernLightApi.jar").asFile, true)
    }

    build {
        dependsOn(shadowJar)
    }

    /*
    reobfJar {
      // This is an example of how you might change the output location for reobfJar. It's recommended not to do this
      // for a variety of reasons, however it's asked frequently enough that an example of how to do it is included here.
      outputJar.set(layout.buildDirectory.file("libs/PaperweightTestPlugin-${project.version}.jar"))
    }
     */
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

// Configure plugin.yml generation
bukkit {
    load = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder.STARTUP
    main = "com.boy0000.modernlightapi.ModernLightApi"
    version = "${project.version}"
    apiVersion = "1.19"
    authors = listOf("Author")
}
