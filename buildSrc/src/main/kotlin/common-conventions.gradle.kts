plugins {
    kotlin("jvm")
    id("jacoco")
    id("com.github.nbaztec.coveralls-jacoco")
    id("org.jetbrains.dokka")
}

repositories {
    mavenLocal()
    mavenCentral()
}

val abaktVersion = providers.gradleProperty("abaktVersion").get()
group = "works.resolute"
version = abaktVersion

kotlin {
    jvmToolchain(21)
}

java {
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    val kotlinVersion = providers.gradleProperty("kotlinVersion").get()

    implementation("io.github.oshai:kotlin-logging-jvm:8.0.03")
    implementation("org.slf4j:slf4j-api:2.0.18")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
}

jacoco {
    // The org.jacoco.core jar on the buildSrc classpath is the single JaCoCo pin; its VERSION carries a
    // build timestamp (0.8.15.2026...) that the published agent and ant artifacts do not.
    toolVersion = org.jacoco.core.JaCoCo.VERSION.substringBeforeLast(".")
}

dokka {
    dokkaPublications.html {
        suppressInheritedMembers.set(true)
    }
}
