plugins {
    id("common-conventions")
    id("test-conventions")
    id("publish-conventions")
}

dependencies {
    testImplementation(project(":abakt-test"))
}
