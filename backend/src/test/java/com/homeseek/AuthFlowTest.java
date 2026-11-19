package com.homeseek;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeseek.model.User;
import com.homeseek.repository.UserRepository;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** End-to-end checks of registration, login, logout, and role rules against a real (in-memory) database. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;

    private ResultActions postJson(String url, Object body) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private ResultActions register(String email, String role) throws Exception {
        return postJson("/api/auth/register", Map.of("fullName", "Juan Dela Cruz", "email", email,
                "phone", "09171234567", "password", "Passw0rd123", "role", role));
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

    @Test
    void registerStoresHashedPasswordAndRole() throws Exception {
        register("tenant1@test.local", "TENANT").andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("TENANT"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        User saved = users.findByEmail("tenant1@test.local").orElseThrow();
        assertThat(saved.getPasswordHash()).startsWith("$2").isNotEqualTo("Passw0rd123");
        register("owner1@test.local", "OWNER").andExpect(jsonPath("$.role").value("OWNER"));
    }

    @Test
    void registerRejectsDuplicateEmailIgnoringCase() throws Exception {
        register("dup@test.local", "TENANT").andExpect(status().isCreated());
        register("DUP@Test.Local", "OWNER").andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void registerValidatesFieldsAndNeverAllowsAdmin() throws Exception {
        postJson("/api/auth/register", Map.of("fullName", "A", "email", "bad", "phone", "x",
                "password", "short", "role", "TENANT")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fullName").exists()).andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.phone").exists()).andExpect(jsonPath("$.errors.password").exists());
        postJson("/api/auth/register", Map.of("fullName", "Weak Password", "email", "weak@test.local",
                "phone", "09171234567", "password", "onlyletters", "role", "TENANT"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.password").exists());
        register("sneaky@test.local", "ADMIN").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.role").exists());
        assertThat(users.existsByEmail("sneaky@test.local")).isFalse();
    }

    @Test
    void loginSucceedsAndReturnsUsableToken() throws Exception {
        register("login1@test.local", "TENANT").andExpect(status().isCreated());
        login("Login1@test.local", "Passw0rd123").andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty()).andExpect(jsonPath("$.user.role").value("TENANT"));
        getWith("/api/auth/me", token("login1@test.local", "Passw0rd123")).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("login1@test.local"));
    }

    @Test
    void invalidCredentialsGiveTheSameGenericError() throws Exception {
        register("bad1@test.local", "TENANT").andExpect(status().isCreated());
        login("bad1@test.local", "WrongPass1").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
        login("nobody@test.local", "WrongPass1").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void fiveFailedLoginsLockTheAccountEvenForTheCorrectPassword() throws Exception {
        register("lock1@test.local", "TENANT").andExpect(status().isCreated());
        for (int i = 0; i < 5; i++) {
            login("lock1@test.local", "WrongPass1").andExpect(status().isUnauthorized());
        }
        login("lock1@test.local", "Passw0rd123").andExpect(status().isTooManyRequests());
    }

    @Test
    void deactivatedAccountCannotLogIn() throws Exception {
        register("off1@test.local", "OWNER").andExpect(status().isCreated());
        User u = users.findByEmail("off1@test.local").orElseThrow();
        u.setActive(false);
        users.save(u);
        login("off1@test.local", "Passw0rd123").andExpect(status().isForbidden());
    }

    @Test
    void logoutEndsTheSession() throws Exception {
        register("out1@test.local", "TENANT").andExpect(status().isCreated());
        String t = token("out1@test.local", "Passw0rd123");
        getWith("/api/tenant/dashboard", t).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + t)).andExpect(status().isNoContent());
        getWith("/api/tenant/dashboard", t).andExpect(status().isUnauthorized());
        token("out1@test.local", "Passw0rd123");   // logging in again gives a fresh, working token
    }

    @Test
    void roleRulesAreEnforcedOnTheServer() throws Exception {
        register("rbac-t@test.local", "TENANT").andExpect(status().isCreated());
        register("rbac-o@test.local", "OWNER").andExpect(status().isCreated());
        String tenant = token("rbac-t@test.local", "Passw0rd123");
        String owner = token("rbac-o@test.local", "Passw0rd123");
        String admin = token("admin@test.local", "AdminPass123");   // seeded from configuration

        mvc.perform(get("/api/tenant/dashboard")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        getWith("/api/tenant/dashboard", tenant).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("TENANT"));
        getWith("/api/owner/dashboard", tenant).andExpect(status().isForbidden());
        getWith("/api/admin/dashboard", tenant).andExpect(status().isForbidden());
        getWith("/api/owner/dashboard", owner).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("OWNER"));
        getWith("/api/tenant/dashboard", owner).andExpect(status().isForbidden());
        getWith("/api/admin/dashboard", owner).andExpect(status().isForbidden());
        getWith("/api/admin/dashboard", admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.totalUsers").isNumber()).andExpect(jsonPath("$.role").value("ADMIN"));
        getWith("/api/owner/dashboard", admin).andExpect(status().isForbidden());
    }

    @Test
    void tamperedOrGarbageTokensAreRejected() throws Exception {
        register("tamper@test.local", "TENANT").andExpect(status().isCreated());
        String t = token("tamper@test.local", "Passw0rd123");
        getWith("/api/auth/me", t + "x").andExpect(status().isUnauthorized());
        getWith("/api/auth/me", "not-a-jwt").andExpect(status().isUnauthorized());
    }
}
