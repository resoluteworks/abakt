plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    val kotlinVersion = "2.0.20"

    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${kotlinVersion}")
    implementation("org.jacoco:org.jacoco.core:0.8.12")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:2.0.0")
    implementation("com.github.nbaztec:coveralls-jacoco-gradle-plugin:1.2.18")
    implementation("com.gradleup.nmcp:com.gradleup.nmcp.gradle.plugin:0.0.9")
}
