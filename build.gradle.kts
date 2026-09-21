plugins {
    base
    id("org.jetbrains.dokka")
    id("com.gradleup.nmcp.aggregation")
}

group = "works.resolute"

repositories {
    mavenCentral()
}

nmcpAggregation {
    centralPortal {
        username = System.getenv("SONATYPE_PUBLISH_USERNAME")
        password = System.getenv("SONATYPE_PUBLISH_PASSWORD")
        publishingType = "AUTOMATIC"
    }
}

dokka {
    dokkaPublications.html {
        outputDirectory.set(layout.projectDirectory.dir("docs/dokka"))
    }
}

dependencies {
    dokka(project(":abakt-core"))
    dokka(project(":abakt-test"))
    nmcpAggregation(project(":abakt-core"))
    nmcpAggregation(project(":abakt-test"))
}
