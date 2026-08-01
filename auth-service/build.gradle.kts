plugins {
    id("socialmedia.spring-service-conventions")
}

description = "Auth Service"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.kafka:spring-kafka")

    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Two-factor authentication (TOTP) + QR code rendering for authenticator apps and QR login
    implementation("dev.samstevens.totp:totp:1.7.1")
    implementation("com.google.zxing:core:3.5.4")
    implementation("com.google.zxing:javase:3.5.4")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("com.redis:testcontainers-redis:2.2.4")
    // TEST-ONLY, deliberately: the real Signal Protocol library, used by the reference-client
    // test to play the role a mobile client's crypto layer would. Production code in this
    // service must never depend on it - auth-service only stores and serves opaque public key
    // bytes and holds no cryptographic logic, exactly like Signal's own server.
    testImplementation("org.signal:libsignal-client:0.86.5")
}

// Unit tests (*Test) need no external services and run under the normal `test` task.
// Integration tests (*IT) spin up real Postgres/Redis via Testcontainers and require a
// Docker daemon - they're excluded from `test` and only run via `./gradlew integrationTest`.
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
