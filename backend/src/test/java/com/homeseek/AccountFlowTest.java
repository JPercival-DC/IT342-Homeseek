package com.homeseek;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeseek.model.PasswordResetToken;
import com.homeseek.model.User;
import com.homeseek.repository.PasswordResetTokenRepository;
import com.homeseek.repository.UserRepository;
import com.homeseek.service.PasswordResetMailer;
import com.homeseek.service.PasswordService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Increment 2: profile, change password, forgot/reset password. The database is a real in-memory one. Email sending is
 * mocked (PasswordResetMailer), so no Gmail account is needed: the tests read the reset link from the mock's arguments.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountFlowTest {

    private static final String PW = "Passw0rd123";

    @MockitoBean PasswordResetMailer mailer;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired PasswordResetTokenRepository tokens;

    @BeforeEach
    void mailIsConfigured() {
        when(mailer.configured()).thenReturn(true);
    }

    // ---------- helpers ----------

    private String newUser() throws Exception {
        String email = "u" + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
        postJson("/api/auth/register", Map.of("fullName", "Juan Dela Cruz", "email", email, "phone", "09171234567",
                "password", PW, "role", "TENANT")).andExpect(status().isCreated());
        return email;
    }

    private ResultActions postJson(String url, Object body) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private ResultActions send(MockHttpServletRequestBuilder req, String token, Object body) throws Exception {
        if (token != null) {
            req.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private ResultActions login(String email, String password) throws Exception {
        return postJson("/api/auth/login", Map.of("email", email, "password", password));
    }

    private String token(String email, String password) throws Exception {
        String body = login(email, password).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    private ResultActions getWith(String url, String token) throws Exception {
        return mvc.perform(get(url).header("Authorization", "Bearer " + token));
    }

    /**
     * Asks for a reset link and returns the token from the "email". The sending runs in the background, so the mock is
     * checked with a timeout. nth = how many emails this address should have received by now.
     */
    private String requestLink(String email, int nth) throws Exception {
        postJson("/api/password/forgot", Map.of("email", email)).andExpect(status().isNoContent());
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(mailer, timeout(5000).times(nth)).send(eq(email), link.capture(), eq(30L));
        String sent = link.getAllValues().get(nth - 1);
        assertThat(sent).contains("/reset-password?token=");
        return sent.substring(sent.indexOf("token=") + "token=".length());
    }

    private ResultActions reset(String token, String newPassword) throws Exception {
        return postJson("/api/password/reset", Map.of("token", token, "newPassword", newPassword));
    }

    // ---------- profile ----------

    @Test
    void ownerCanViewAndUpdateOwnProfileButNotEmailOrRole() throws Exception {
        String email = newUser();
        String t = token(email, PW);
        getWith("/api/profile", t).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email)).andExpect(jsonPath("$.fullName").value("Juan Dela Cruz"));

        send(put("/api/profile"), t, Map.of("fullName", "  Maria Santos ", "phone", "+63 917 123 4567",
                "email", "hacker@test.local", "role", "ADMIN"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value("Maria Santos"))
                .andExpect(jsonPath("$.role").value("TENANT"));

        User saved = users.findByEmail(email).orElseThrow();
        assertThat(saved.getFullName()).isEqualTo("Maria Santos");
        assertThat(saved.getPhone()).isEqualTo("+63 917 123 4567");
        assertThat(saved.getRole().name()).isEqualTo("TENANT");
        assertThat(users.findByEmail("hacker@test.local")).isEmpty();
        getWith("/api/profile", t).andExpect(jsonPath("$.fullName").value("Maria Santos"));   // persisted, not echoed
    }

    @Test
    void profileUpdateIsValidated() throws Exception {
        String email = newUser();
        String t = token(email, PW);
        send(put("/api/profile"), t, Map.of("fullName", " ", "phone", "abc")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fullName").exists()).andExpect(jsonPath("$.errors.phone").exists());
        assertThat(users.findByEmail(email).orElseThrow().getFullName()).isEqualTo("Juan Dela Cruz");
    }

    @Test
    void profileNeedsLoginAndCanNeverReachAnotherUser() throws Exception {
        mvc.perform(get("/api/profile")).andExpect(status().isUnauthorized());
        send(put("/api/profile"), null, Map.of("fullName", "Anon", "phone", "09171234567")).andExpect(status().isUnauthorized());
        getWith("/api/profile", "garbage.token.value").andExpect(status().isUnauthorized());

        String a = newUser();
        String b = newUser();
        long bId = users.findByEmail(b).orElseThrow().getId();
        send(put("/api/profile"), token(a, PW), Map.of("id", bId, "userId", bId, "email", b,
                "fullName", "Only A Changes", "phone", "09170000000")).andExpect(status().isOk());

        assertThat(users.findByEmail(a).orElseThrow().getFullName()).isEqualTo("Only A Changes");
        User untouched = users.findByEmail(b).orElseThrow();
        assertThat(untouched.getFullName()).isEqualTo("Juan Dela Cruz");
        assertThat(untouched.getPhone()).isEqualTo("09171234567");
    }

    // ---------- change password ----------

    @Test
    void changePasswordNeedsTheCurrentPasswordAndEndsOlderSessions() throws Exception {
        String email = newUser();
        String t = token(email, PW);
        String url = "/api/password/change";

        send(post(url), t, Map.of("currentPassword", "Wrong1234", "newPassword", "NewPassw0rd1"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.currentPassword").exists());
        getWith("/api/profile", t).andExpect(status().isOk());                    // a typo does not log the user out
        send(post(url), t, Map.of("currentPassword", PW, "newPassword", "short")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.newPassword").exists());
        send(post(url), t, Map.of("currentPassword", PW, "newPassword", PW)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.newPassword").exists());

        send(post(url), t, Map.of("currentPassword", PW, "newPassword", "NewPassw0rd1")).andExpect(status().isNoContent());

        assertThat(users.findByEmail(email).orElseThrow().getPasswordHash()).startsWith("$2");
        getWith("/api/profile", t).andExpect(status().isUnauthorized());          // the old session ended
        login(email, PW).andExpect(status().isUnauthorized());                    // the old password is gone
        getWith("/api/profile", token(email, "NewPassw0rd1")).andExpect(status().isOk());
    }

    @Test
    void changePasswordRequiresLoginAndLimitsWrongGuesses() throws Exception {
        send(post("/api/password/change"), null, Map.of("currentPassword", PW, "newPassword", "NewPassw0rd1"))
                .andExpect(status().isUnauthorized());

        String email = newUser();
        String t = token(email, PW);
        for (int i = 0; i < 5; i++) {
            send(post("/api/password/change"), t, Map.of("currentPassword", "Wrong1234", "newPassword", "NewPassw0rd1"))
                    .andExpect(status().isBadRequest());
        }
        send(post("/api/password/change"), t, Map.of("currentPassword", PW, "newPassword", "NewPassw0rd1"))
                .andExpect(status().isTooManyRequests());
    }

    // ---------- forgot password ----------

    @Test
    void forgotPasswordEmailsRealAccountsOnlyAndStoresOnlyAHashedToken() throws Exception {
        String known = newUser();
        String token = requestLink(known, 1);

        assertThat(token).hasSizeGreaterThanOrEqualTo(43);                       // 256 random bits, Base64url
        assertThat(tokens.findByTokenHash(token)).isEmpty();                     // the raw token is not stored
        PasswordResetToken row = tokens.findByTokenHash(PasswordService.hashToken(token)).orElseThrow();
        assertThat(row.getTokenHash()).hasSize(64).isNotEqualTo(token);
        assertThat(Duration.between(row.getCreatedAt(), row.getExpiresAt())).isEqualTo(Duration.ofMinutes(30));
        assertThat(row.getUsedAt()).isNull();
        assertThat(row.getUserId()).isEqualTo(users.findByEmail(known).orElseThrow().getId());

        String unknown = "nobody" + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
        String disabled = newUser();
        User u = users.findByEmail(disabled).orElseThrow();
        u.setActive(false);
        users.save(u);
        postJson("/api/password/forgot", Map.of("email", unknown)).andExpect(status().isNoContent());   // same answer
        postJson("/api/password/forgot", Map.of("email", disabled.toUpperCase())).andExpect(status().isNoContent());
        Thread.sleep(500);                                                       // the background task has had time to run
        verify(mailer, never()).send(eq(unknown), org.mockito.ArgumentMatchers.any(), anyLong());
        verify(mailer, never()).send(eq(disabled), org.mockito.ArgumentMatchers.any(), anyLong());

        postJson("/api/password/forgot", Map.of("email", "not-an-email")).andExpect(status().isBadRequest());
    }

    @Test
    void forgotPasswordIsUnavailableUntilGmailIsConfigured() throws Exception {
        when(mailer.configured()).thenReturn(false);
        postJson("/api/password/forgot", Map.of("email", newUser())).andExpect(status().isServiceUnavailable());
    }

    // ---------- reset password ----------

    @Test
    void resetPasswordWithALinkWorksOnceAndEndsOlderSessions() throws Exception {
        String email = newUser();
        String oldSession = token(email, PW);
        for (int i = 0; i < 5; i++) {
            login(email, "Wrong1234").andExpect(status().isUnauthorized());
        }
        login(email, PW).andExpect(status().isTooManyRequests());                 // locked out: the reason to reset

        String link = requestLink(email, 1);
        reset(link, "Brand9NewPass").andExpect(status().isNoContent());

        assertThat(users.findByEmail(email).orElseThrow().getPasswordHash()).startsWith("$2");   // BCrypt, as before
        login(email, "Brand9NewPass").andExpect(status().isOk());                 // new password works and the lock is cleared
        login(email, PW).andExpect(status().isUnauthorized());
        getWith("/api/profile", oldSession).andExpect(status().isUnauthorized()); // token_version ended the old session
        assertThat(tokens.findByTokenHash(PasswordService.hashToken(link)).orElseThrow().getUsedAt()).isNotNull();
        reset(link, "Another9Pass").andExpect(status().isBadRequest());           // the link was used up
        login(email, "Brand9NewPass").andExpect(status().isOk());                 // and did not change anything
    }

    @Test
    void resetPasswordRejectsUnknownExpiredWeakAndDisabled() throws Exception {
        reset("x".repeat(43), "Brand9NewPass").andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(
                "This reset link is invalid or has expired. Please request a new one."));

        String email = newUser();
        String link = requestLink(email, 1);
        reset(link, "weak").andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.newPassword").exists());
        login(email, PW).andExpect(status().isOk());                              // nothing changed
        // a weak password did not use up the link:
        long userId = users.findByEmail(email).orElseThrow().getId();
        assertThat(tokens.findByTokenHash(PasswordService.hashToken(link)).orElseThrow().getUsedAt()).isNull();

        String expiredToken = "expired-" + UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        tokens.save(new PasswordResetToken(userId, PasswordService.hashToken(expiredToken), now.minusMinutes(1), now.minusMinutes(31)));
        reset(expiredToken, "Brand9NewPass").andExpect(status().isBadRequest());
        login(email, PW).andExpect(status().isOk());

        User u = users.findByEmail(email).orElseThrow();
        u.setActive(false);
        users.save(u);
        reset(link, "Brand9NewPass").andExpect(status().isBadRequest());          // disabled accounts cannot be recovered
        assertThat(tokens.findByTokenHash(PasswordService.hashToken(link)).orElseThrow().getUsedAt()).isNull();  // rolled back
    }

    @Test
    void aNewerLinkReplacesTheOlderOne() throws Exception {
        String email = newUser();
        String first = requestLink(email, 1);
        String second = requestLink(email, 2);
        assertThat(second).isNotEqualTo(first);
        reset(first, "Brand9NewPass").andExpect(status().isBadRequest());
        reset(second, "Brand9NewPass").andExpect(status().isNoContent());
    }

    @Test
    void changingThePasswordCancelsOpenResetLinks() throws Exception {
        String email = newUser();
        String link = requestLink(email, 1);
        send(post("/api/password/change"), token(email, PW), Map.of("currentPassword", PW, "newPassword", "NewPassw0rd1"))
                .andExpect(status().isNoContent());
        reset(link, "Brand9NewPass").andExpect(status().isBadRequest());
        login(email, "NewPassw0rd1").andExpect(status().isOk());
    }
}
