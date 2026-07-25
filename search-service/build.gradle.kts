plugins {
    id("socialmedia.spring-service-conventions")
}

description = "Search Service"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.opensearch.client:opensearch-java:3.9.0")
    implementation("org.opensearch.client:opensearch-rest-client:3.7.0")
    implementation("org.apache.httpcomponents.client5:httpclient5")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
}

tasks.test {
    useJUnitPlatform()
}
