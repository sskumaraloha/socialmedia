package com.socialmedia.notification.controller;

import com.socialmedia.notification.dto.request.RegisterDeviceTokenRequest;
import com.socialmedia.notification.dto.request.SendNotificationRequest;
import com.socialmedia.notification.dto.response.NotificationLogResponse;
import com.socialmedia.notification.service.NotificationService;
import com.socialmedia.common.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/device-tokens")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registerDeviceToken(@Valid @RequestBody RegisterDeviceTokenRequest request) {
        notificationService.registerDeviceToken(CurrentUser.get().userId(), request);
    }

    @DeleteMapping("/device-tokens/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unregisterDeviceToken(@PathVariable String token) {
        notificationService.unregisterDeviceToken(CurrentUser.get().userId(), token);
    }

    @PostMapping("/send")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void requestSend(@Valid @RequestBody SendNotificationRequest request) {
        notificationService.requestSend(CurrentUser.get().userId(), request);
    }

    @GetMapping("/logs")
    public List<NotificationLogResponse> listLogs() {
        return notificationService.listLogs(CurrentUser.get().userId());
    }
}
