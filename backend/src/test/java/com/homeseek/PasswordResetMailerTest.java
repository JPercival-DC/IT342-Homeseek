package com.homeseek;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.homeseek.service.PasswordResetMailer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/** The reset email itself, with the mail server mocked. */
class PasswordResetMailerTest {

    @Test
    void emailContainsTheLinkTheExpiryAndComesFromTheGmailAccount() {
        JavaMailSender sender = mock(JavaMailSender.class);
        new PasswordResetMailer(sender, "homeseek.app@gmail.com", "abcdefghijklmnop")
                .send("tenant@test.local", "http://localhost:5173/reset-password?token=abc", 30);

        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(sent.capture());
        SimpleMailMessage mail = sent.getValue();
        assertThat(mail.getTo()).containsExactly("tenant@test.local");
        assertThat(mail.getFrom()).contains("homeseek.app@gmail.com");
        assertThat(mail.getSubject()).isEqualTo("Reset your HomeSeek password");
        assertThat(mail.getText()).contains("http://localhost:5173/reset-password?token=abc").contains("30 minutes");
    }

    @Test
    void notConfiguredUntilBothGmailSettingsAreSet() {
        JavaMailSender sender = mock(JavaMailSender.class);
        assertThat(new PasswordResetMailer(sender, "homeseek.app@gmail.com", "abcdefghijklmnop").configured()).isTrue();
        assertThat(new PasswordResetMailer(sender, "", "abcdefghijklmnop").configured()).isFalse();
        assertThat(new PasswordResetMailer(sender, "homeseek.app@gmail.com", "").configured()).isFalse();
    }
}
