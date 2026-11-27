package com.homeseek;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeseek.service.PasswordResetMailer;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** With the real 60-second cooldown, asking twice in a row sends one email. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "homeseek.password-reset.cooldown-seconds=60")
class PasswordResetCooldownTest {

    @MockitoBean PasswordResetMailer mailer;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private void call(String url, Object body) throws Exception {
        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void repeatedRequestsDoNotFloodTheInbox() throws Exception {
        when(mailer.configured()).thenReturn(true);
        String email = "cool" + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
        call("/api/auth/register", Map.of("fullName", "Juan Dela Cruz", "email", email, "phone", "09171234567",
                "password", "Passw0rd123", "role", "TENANT"));

        call("/api/password/forgot", Map.of("email", email));
        verify(mailer, timeout(5000)).send(eq(email), org.mockito.ArgumentMatchers.any(), anyLong());
        call("/api/password/forgot", Map.of("email", email));
        call("/api/password/forgot", Map.of("email", email));
        Thread.sleep(700);
        verify(mailer, times(1)).send(eq(email), org.mockito.ArgumentMatchers.any(), anyLong());
    }
}
