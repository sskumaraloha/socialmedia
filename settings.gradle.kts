rootProject.name = "socialmedia-platform"

include(
    "common-library",
    "gateway-service",
    "auth-service",
    "user-service",
    "chat-service",
    "message-service",
    "media-service",
    "notification-service",
    "presence-service",
    "search-service",
    "ai-service",
    "payment-service",
    "analytics-service"
)

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
