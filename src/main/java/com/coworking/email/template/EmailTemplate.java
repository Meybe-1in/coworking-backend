package com.coworking.email.template;

public enum EmailTemplate {
    VERIFY_ACCOUNT("email/verification-email"),
    RESET_PASSWORD("email/reset-password");

    private final String path;

    EmailTemplate(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
