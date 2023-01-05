pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }

    plugins {
        kotlin("jvm") version "1.8.0"
    }
}
rootProject.name = "ModernLightApi"
include("common", "bukkit-common", "craftbukkit-common","v1_19_3")
