plugins {
    id("socialmedia.java-conventions")
    `java-library`
}

description = "Shared cross-cutting concerns: exception handling, logging, OpenAPI and observability config."

dependencies {
    api("org.springframework.boot:spring-boot-autoconfigure")
    api("org.springframework:spring-context")
    api("net.logstash.logback:logstash-logback-encoder:8.0")

    compileOnly("org.springframework:spring-web")
    compileOnly("jakarta.servlet:jakarta.servlet-api")
    compileOnly("jakarta.validation:jakarta.validation-api")
    compileOnly("org.springframework.boot:spring-boot-starter-actuator")
    compileOnly("io.micrometer:micrometer-core")
    compileOnly("org.springdoc:springdoc-openapi-starter-common:2.6.0")
    compileOnly("org.springframework.boot:spring-boot-starter-data-jpa")

    annotationProcessor("org.springframework.boot:spring-boot-autoconfigure-processor")

    testImplementation("org.springframework:spring-web")
    testImplementation("jakarta.servlet:jakarta.servlet-api")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
}
