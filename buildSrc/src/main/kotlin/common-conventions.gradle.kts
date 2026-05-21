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

val abaktVersion: String by project
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
    val kotlinVersion: String by project

    implementation("io.github.oshai:kotlin-logging-jvm:8.0.03")
    implementation("org.slf4j:slf4j-api:2.0.18")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
}

tasks.dokkaHtml {
    outputDirectory.set(layout.projectDirectory.dir("../docs/dokka"))
    suppressInheritedMembers = true
}
