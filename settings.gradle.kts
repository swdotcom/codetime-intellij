pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // Enables automatic provisioning of required JDK toolchains (e.g., Java 17 for IntelliJ 2025.3+).
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "codetime"
