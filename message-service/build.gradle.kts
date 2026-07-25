plugins {
    id("socialmedia.spring-service-conventions")
}

description = "Message Service"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
    // Resilience4j's Circuit Breaker + Retry + Bulkhead, via annotations, wrapping the one
    // synchronous inter-service call in this platform (ChatMembershipClient -> chat-service).
    // spring-boot-starter-aop is required alongside resilience4j-spring-boot3 - without it,
    // @CircuitBreaker/@Retry/@Bulkhead are silently inert (no AspectJ weaver to create the
    // proxy that actually intercepts annotated method calls).
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
    implementation("org.springframework.boot:spring-boot-starter-aop")

    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:mongodb")
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
