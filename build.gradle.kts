plugins {
    base
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

dependencies {
    nmcpAggregation(project(":abakt-core"))
    nmcpAggregation(project(":abakt-test"))
}
