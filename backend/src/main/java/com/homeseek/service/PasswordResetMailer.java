package com.homeseek.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Sends the reset email through Gmail SMTP (settings under spring.mail in application.yml, the Google App Password
 * comes from the environment). The only class that talks to the mail server, so tests replace it with a mock.
 */
@Component
public class PasswordResetMailer {

    private final JavaMailSender sender;
    private final String username;
    private final String appPassword;

    public PasswordResetMailer(JavaMailSender sender, @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String appPassword) {
        this.sender = sender;
        this.username = username;
        this.appPassword = appPassword;
    }

    /** False until MAIL_USERNAME and MAIL_APP_PASSWORD are set. Recovery is then switched off; nothing else is affected. */
    public boolean configured() {
        return !username.isBlank() && !appPassword.isBlank();
    }

    public void send(String to, String link, long validMinutes) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("HomeSeek <" + username + ">");     // Gmail always sends from the authenticated account
        message.setTo(to);
        message.setSubject("Reset your HomeSeek password");
        message.setText("""
                We received a request to reset the password of your HomeSeek account.

                Open this link to choose a new password:
                %s

                The link works once and expires in %d minutes.
                If you did not ask for this, ignore this email: your password has not been changed.
                """.formatted(link, validMinutes));
        sender.send(message);
    }
}
