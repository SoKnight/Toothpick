import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    `kotlin-dsl`
    `maven-publish`
}

group = "xyz.jpenilla"
version = "1.1.0+patch.1"
description = "Gradle plugin to assist in forking Paper"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

kotlin {
    explicitApi()
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.bundles.configurate)
    implementation(libs.bundles.jackson)
    implementation(libs.shadow)
}

tasks {
    compileKotlin {
        compilerOptions{
            apiVersion.set(KotlinVersion.KOTLIN_2_2)
            jvmTarget.set(JvmTarget.JVM_17)
            languageVersion.set(KotlinVersion.KOTLIN_2_2)
        }
    }

    jar {
        manifest.attributes(mapOf(
            "Implementation-Version" to project.version
        ))
    }
}

gradlePlugin {
    plugins {
        create("Toothpick") {
            id = "xyz.jpenilla.toothpick"
            implementationClass = "xyz.jpenilla.toothpick.Toothpick"
        }
    }
}
