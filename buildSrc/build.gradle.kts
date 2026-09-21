import java.util.Properties

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

val kotlinVersion: String = Properties().apply {
    rootDir.parentFile.resolve("gradle.properties").inputStream().use { load(it) }
}.getProperty("kotlinVersion") ?: error("kotlinVersion missing from gradle.properties")

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${kotlinVersion}")
    implementation("org.jacoco:org.jacoco.core:0.8.15")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:2.2.0")
    implementation("com.github.nbaztec:coveralls-jacoco-gradle-plugin:1.2.20")
    implementation("com.gradleup.nmcp:com.gradleup.nmcp.gradle.plugin:1.6.2")
    implementation("com.gradleup.nmcp.aggregation:com.gradleup.nmcp.aggregation.gradle.plugin:1.6.2")
}
