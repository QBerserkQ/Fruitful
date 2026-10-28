package volodea.fruitful.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import volodea.fruitful.AbstractIntegrationTest;
import volodea.fruitful.repository.RefreshTokenRepository;
import volodea.fruitful.repository.UserRepository;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    UserRepository userRepository;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    private static final String EMAIL = "vova@test.com";
    private static final String PASSWORD = "Password123!";

    @BeforeEach
    void cleanDb() {
        refreshTokenRepository.deleteAll(); // сначала токены из-за FK
        userRepository.deleteAll();
    }

    @Test
    void register_returnsTokens() throws Exception {
        post("/api/auth/register", registerBody(EMAIL))
                .andExpect(status().is2xxSuccessful()) // поставь точно 200 или 201
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        post("/api/auth/register", registerBody(EMAIL)).andExpect(status().is2xxSuccessful());
        post("/api/auth/register", registerBody(EMAIL)).andExpect(status().isConflict());
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        post("/api/auth/register", registerBody("not-an-email"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        post("/api/auth/register", registerBody(EMAIL)).andExpect(status().is2xxSuccessful());
        post("/api/auth/login", Map.of("email", EMAIL, "password", "wrong-password"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_correctPassword_returnsTokens() throws Exception {
        post("/api/auth/register", registerBody(EMAIL)).andExpect(status().is2xxSuccessful());
        post("/api/auth/login", Map.of("email", EMAIL, "password", PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void refresh_returnsNewPair() throws Exception {
        String oldRefresh = registerAndGetRefreshToken();

        post("/api/auth/refresh", Map.of("refreshToken", oldRefresh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void refresh_oldTokenAfterRotation_returns401() throws Exception {
        String oldRefresh = registerAndGetRefreshToken();

        post("/api/auth/refresh", Map.of("refreshToken", oldRefresh)).andExpect(status().isOk());
        post("/api/auth/refresh", Map.of("refreshToken", oldRefresh)).andExpect(status().isUnauthorized());
    }

    private Map<String, String> registerBody(String email) {
        return Map.of("username", "vova", "email", email, "password", PASSWORD);
    }

    private ResultActions post(String url, Object body) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private String registerAndGetRefreshToken() throws Exception {
        String json = post("/api/auth/register", registerBody(EMAIL))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("refreshToken").asText();
    }
}