package com.socialmedia.notification.service.impl;

import com.socialmedia.notification.service.SmsSender;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TwilioSmsSender implements SmsSender {

    private final String fromNumber;

    public TwilioSmsSender(@Value("${app.notification.sms.account-sid:}") String accountSid,
            @Value("${app.notification.sms.auth-token:}") String authToken,
            @Value("${app.notification.sms.from-number:}") String fromNumber) {
        if (!accountSid.isBlank() && !authToken.isBlank()) {
            Twilio.init(accountSid, authToken);
        }
        this.fromNumber = fromNumber;
    }

    @Override
    public void send(String toPhoneNumber, String body) {
        Message.creator(new PhoneNumber(toPhoneNumber), new PhoneNumber(fromNumber), body).create();
    }
}
