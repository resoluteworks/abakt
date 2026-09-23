plugins {
    id("common-conventions")
    id("test-conventions")
    id("publish-conventions")
}

description = "Kotest matchers and utilities for writing tests against abakt authorization policies."

dependencies {
    val kotestVersion = providers.gradleProperty("kotestVersion").get()
    val mockkVersion = providers.gradleProperty("mockkVersion").get()

    implementation(project(":abakt-core"))

    implementation("io.kotest:kotest-assertions-core:${kotestVersion}")
    implementation("io.kotest:kotest-property:${kotestVersion}")
    implementation("io.kotest:kotest-runner-junit5-jvm:${kotestVersion}")
    implementation("io.mockk:mockk:${mockkVersion}")
}
