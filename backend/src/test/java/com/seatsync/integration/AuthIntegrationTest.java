package com.seatsync.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.seatsync.dto.verification.VerificationCode;
import com.seatsync.entity.Role;
import com.seatsync.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends AbstractIntegrationTest {

    private static final Map<String, Object> ASHA = Map.of("name", "Asha Rao", "email", "Asha@Example.com", "password", "Secret123");

    @Test
    void registrationEmailsACodeAndOnlyStartsASessionOnceVerified() throws Exception {
        JsonNode verification = startRegistration(ASHA)
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.verification.required").value(true))
                .andExpect(jsonPath("$.verification.maskedEmail").value("a***a@example.com"))
                .andReturn().getResponse().getContentAsString().transform(this::verificationOf);
        assertThat(emailVerified("asha@example.com")).isFalse();

        mockMvc.perform(post("/api/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(codeFor(verification, "asha@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("asha@example.com"))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")));

        assertThat(emailVerified("asha@example.com")).isTrue();
        String storedHash = jdbc.queryForObject("SELECT password_hash FROM users WHERE email = 'asha@example.com'", String.class);
        assertThat(storedHash).startsWith("$2").doesNotContain("Secret123");
    }

    @Test
    void verifiedEmailCannotBeRegisteredAgain() throws Exception {
        JsonNode verification = verificationOf(startRegistration(ASHA).andReturn().getResponse().getContentAsString());
        mockMvc.perform(post("/api/auth/verify").contentType(MediaType.APPLICATION_JSON)
                        .content(json(codeFor(verification, "asha@example.com"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(ASHA)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("An account with this email already exists."));
    }

    @Test
    void unverifiedRegistrationCanBeRestartedSoNobodyCanSquatAnEmail() throws Exception {
        startRegistration(Map.of("name", "Squatter", "email", "asha@example.com", "password", "Squat1234"));
        JsonNode second = verificationOf(startRegistration(ASHA).andReturn().getResponse().getContentAsString());

        mockMvc.perform(post("/api/auth/verify").contentType(MediaType.APPLICATION_JSON)
                        .content(json(codeFor(second, "asha@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.name").value("Asha Rao"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE email = 'asha@example.com'", Long.class)).isEqualTo(1L);
    }

    @Test
    void invalidRegistrationReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "", "email", "not-an-email", "password", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors.email").value("Enter a valid email address"))
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void wrongPasswordIsUnauthorizedAndSendsNoCode() throws Exception {
        createUser("user@test.dev", Role.USER);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "user@test.dev", "password", "WrongPass1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
        assertThat(otpNotifier.sentCount()).isZero();
    }

    @Test
    void passwordAloneDoesNotSignInButTheEmailedCodeDoes() throws Exception {
        createUser("user@test.dev", Role.USER);
        MvcResult passwordStep = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "user@test.dev", "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.verification.purpose").value("LOGIN"))
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andReturn();

        MvcResult codeStep = mockMvc.perform(post("/api/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(codeFor(verificationOf(passwordStep.getResponse().getContentAsString()), "user@test.dev"))))
                .andExpect(status().isOk())
                .andReturn();
        Cookie session = codeStep.getResponse().getCookie("seatsync_token");
        assertThat(session).isNotNull();
        assertThat(session.isHttpOnly()).isTrue();

        mockMvc.perform(get("/api/auth/me").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@test.dev"));
    }

    @Test
    void protectedEndpointWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/bookings/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    private org.springframework.test.web.servlet.ResultActions startRegistration(Map<String, Object> body) throws Exception {
        return mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isCreated());
    }

    private JsonNode verificationOf(String body) {
        try {
            return readJson(body).get("verification");
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private VerificationCode codeFor(JsonNode verification, String email) {
        return new VerificationCode(UUID.fromString(verification.get("challengeId").asText()), otpNotifier.lastCodeFor(email));
    }

    private boolean emailVerified(String email) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT email_verified FROM users WHERE email = ?", Boolean.class, email));
    }
}
