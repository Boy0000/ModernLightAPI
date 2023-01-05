plugins {
    id("org.jetbrains.kotlin.jvm") version "1.8.0"
}

dependencies {
    implementation(project(mapOf("path" to ":bukkit-common")))
    implementation(project(mapOf("path" to ":common")))
    compileOnly("io.papermc.paper:paper-api:1.19.3-R0.1-SNAPSHOT")
}
