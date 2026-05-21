plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    val kotlinVersion = "2.3.21"

    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${kotlinVersion}")
    implementation("org.jacoco:org.jacoco.core:0.8.14")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:2.2.0")
    implementation("com.github.nbaztec:coveralls-jacoco-gradle-plugin:1.2.20")
    implementation("com.gradleup.nmcp:com.gradleup.nmcp.gradle.plugin:1.5.0")
    implementation("com.gradleup.nmcp.aggregation:com.gradleup.nmcp.aggregation.gradle.plugin:1.5.0")
}
