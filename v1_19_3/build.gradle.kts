plugins {
    id("org.jetbrains.kotlin.jvm") version "1.8.0"
    id("io.papermc.paperweight.userdev") version "1.4.0"
}

dependencies {
    implementation(project(mapOf("path" to ":bukkit-common")))
    implementation(project(mapOf("path" to ":common")))
    implementation(project(mapOf("path" to ":craftbukkit-common")))
    compileOnly("io.papermc.paper:paper-api:1.19.3-R0.1-SNAPSHOT")
    paperDevBundle("1.19.3-R0.1-SNAPSHOT")
}

tasks {

    // Configure reobfJar to run when invoking the build task
    assemble {
        dependsOn(reobfJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
        options.release.set(17)
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
    }

    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        filteringCharset = Charsets.UTF_8.name()
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}
