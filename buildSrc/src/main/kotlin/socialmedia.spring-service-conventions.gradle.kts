plugins {
    id("socialmedia.java-conventions")
    id("org.springframework.boot")
}

dependencies {
    "implementation"(project(":common-library"))

    "implementation"("org.springframework.boot:spring-boot-starter-actuator")
    "implementation"("org.springframework.boot:spring-boot-starter-validation")

    "implementation"("io.micrometer:micrometer-registry-prometheus")
    "implementation"("io.micrometer:micrometer-tracing-bridge-brave")
    "implementation"("io.zipkin.reporter2:zipkin-reporter-brave")
}
