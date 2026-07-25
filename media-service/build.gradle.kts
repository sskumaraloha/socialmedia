plugins {
    id("socialmedia.spring-service-conventions")
}

description = "Media Service"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.kafka:spring-kafka")
    implementation(platform("software.amazon.awssdk:bom:2.49.2"))
    implementation("software.amazon.awssdk:s3")
    // "s3" alone pulls in the Apache5-based sync HTTP client, whose httpclient5 version
    // gets conflict-resolved down to 5.3.1 by Spring Boot's own dependency-management BOM
    // (imported for unrelated reasons by the shared convention plugin) - 5.3.1 predates
    // TlsSocketStrategy, so the Apache5 client fails with NoClassDefFoundError at S3Client
    // construction time. The URL Connection client sidesteps the conflict entirely: no
    // extra third-party HTTP library, nothing else in the build graph to fight over its
    // version.
    implementation("software.amazon.awssdk:url-connection-client")
    implementation("net.coobird:thumbnailator:0.4.20")
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
}

tasks.test {
    useJUnitPlatform()
    exclude("**/*IT.class")
}

tasks.register<Test>("integrationTest") {
    description = "Runs Testcontainers-backed integration tests (requires a Docker daemon)."
    group = "verification"
    useJUnitPlatform()
    include("**/*IT.class")
    shouldRunAfter(tasks.test)
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
}
