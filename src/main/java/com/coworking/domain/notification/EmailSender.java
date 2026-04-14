package com.coworking.domain.notification;

import java.util.Map;

public interface EmailSender {
    void send (String to, String subject, String template, Map<String, Object> variables);
}
