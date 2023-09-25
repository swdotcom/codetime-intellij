plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.0"
    id("org.jetbrains.intellij") version "1.15.0"
}

group = "com.software.codetime"
version = "2.8.33"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.snowplowanalytics:snowplow-java-tracker:1.0.0")
    implementation("com.snowplowanalytics:snowplow-java-tracker:1.0.0") {
        capabilities {
            requireCapability("com.snowplowanalytics:snowplow-java-tracker-okhttp-support")
        }
    }
    implementation("com.neovisionaries", "nv-websocket-client", "1.29")
    implementation("org.slf4j:slf4j-api:2.0.7")
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2022.2.5")
    type.set("IC") // Target IDE Platform

    plugins.set(listOf(/* Plugin Dependencies */))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("222")
        untilBuild.set("232.*")
    }
}
