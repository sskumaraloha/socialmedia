package com.socialmedia.notification.service;

public interface EmailSender {

    void send(String to, String subject, String body);
}
