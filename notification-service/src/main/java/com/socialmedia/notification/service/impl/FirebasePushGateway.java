package com.socialmedia.notification.service.impl;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Firebase Cloud Messaging covers both Android push and iOS/APNs delivery - FCM forwards
 * to APNs under the hood for tokens registered from an iOS app, so this one gateway
 * covers both platforms without a separate native APNs HTTP/2 client. If no service
 * account credentials are configured (the case in this sandbox), push is disabled
 * rather than failing application startup.
 */
@Component
public class FirebasePushGateway {

    private static final Logger log = LoggerFactory.getLogger(FirebasePushGateway.class);

    private final FirebaseMessaging messaging;

    public FirebasePushGateway(@Value("${app.notification.firebase.credentials-path:}") String credentialsPath) {
        this.messaging = initialize(credentialsPath);
    }

    private FirebaseMessaging initialize(String credentialsPath) {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.warn("No Firebase credentials configured - push notifications are disabled");
            return null;
        }
        try (InputStream in = new FileInputStream(credentialsPath)) {
            FirebaseOptions options = FirebaseOptions.builder().setCredentials(GoogleCredentials.fromStream(in)).build();
            FirebaseApp app = FirebaseApp.getApps().isEmpty() ? FirebaseApp.initializeApp(options) : FirebaseApp.getInstance();
            return FirebaseMessaging.getInstance(app);
        } catch (IOException e) {
            log.warn("Failed to initialize Firebase - push notifications are disabled: {}", e.getMessage());
            return null;
        }
    }

    public boolean isAvailable() {
        return messaging != null;
    }

    public String send(String token, String title, String body) throws FirebaseMessagingException {
        Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .build();
        return messaging.send(message);
    }
}
