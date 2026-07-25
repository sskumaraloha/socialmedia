package com.socialmedia.notification.service;

public interface SmsSender {

    void send(String toPhoneNumber, String body);
}
