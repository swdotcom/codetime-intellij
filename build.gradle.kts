plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.8.0"
}

group = "com.software.codetime"
version = "2.8.18"

repositories {
    mavenCentral()
}

dependencies {
    implementation(files("lib/swdc-java-ops-1.1.7.jar"))
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2021.3.3")
    type.set("IC") // Target IDE Platform

    plugins.set(listOf(/* Plugin Dependencies */))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    patchPluginXml {
        sinceBuild.set("213")
        untilBuild.set("223.*")
    }
}