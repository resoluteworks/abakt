plugins {
    id("common-conventions")
    id("test-conventions")
    id("publish-conventions")
}

description = "A Kotlin JVM framework for attribute-based access control with a type-safe DSL for declaring and checking authorization policies."

dependencies {
    testImplementation(project(":abakt-test"))
}
