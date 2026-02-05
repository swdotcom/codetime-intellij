import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.0"
    id("org.jetbrains.intellij.platform") version "2.11.0"
}

group = "com.software.codetime"
version = "2.8.44" // bump before publishing

configurations.configureEach {
    // IntelliJ-based IDEs ship Kotlin stdlib; bundling our own copy frequently triggers
    // plugin manager warnings and can cause classpath conflicts.
    exclude(group = "org.jetbrains.kotlin")
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

java {
    toolchain {
        // Build against the oldest supported IDE baseline (2023.2 / 232.*), which uses Java 17.
        // Bytecode built for Java 17 runs on newer IDE runtimes too (e.g., JBR 21 in 2025.3).
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    intellijPlatform {
        // Target the oldest IDE we want to support.
        // This keeps us honest about API availability while still allowing installation on newer IDEs.
        intellijIdea("2023.2.8")
    }

    implementation("com.snowplowanalytics:snowplow-java-tracker:2.1.0")
    implementation("com.snowplowanalytics:snowplow-java-tracker:2.1.0") {
        capabilities {
            requireCapability("com.snowplowanalytics:snowplow-java-tracker-okhttp-support")
        }
    }
    implementation("com.neovisionaries", "nv-websocket-client", "1.29")
    implementation("org.slf4j:slf4j-api:2.0.7")
}

intellijPlatform {
    pluginConfiguration {
        // Build-number compatibility written into the produced plugin.xml.
        // Declare support starting from IntelliJ IDEA 2023.2 (232.*).
        ideaVersion {
            sinceBuild = "232"
            // Do not cap upper bound (allows installing on newer IDE builds too).
            untilBuild = provider { null }
        }
    }

    // Configure IntelliJ Plugin Verifier targets so `./gradlew verifyPlugin` works.
    pluginVerification {
        // The official plugin ID contains "intellij", which the verifier flags as a template word.
        // Keep the ID (changing it would create a "new" plugin), but mute this specific check.
        freeArgs.addAll(listOf("-mute", "TemplateWordInPluginId"))
        ides {
            create(IntelliJPlatformType.IntellijIdeaUltimate, "2023.2.8")
            create(IntelliJPlatformType.IntellijIdeaUltimate, "2025.3.2")
        }
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        // IntelliJ 2023.2+ baseline bytecode.
        options.release.set(17)
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        // No Kotlin sources currently, but keep this valid for Kotlin 1.9.x.
        kotlinOptions.jvmTarget = "17"
    }
}
