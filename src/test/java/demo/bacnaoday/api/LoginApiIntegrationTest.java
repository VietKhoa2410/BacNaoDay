package demo.bacnaoday.api;

import demo.bacnaoday.repository.UserRepository;
import demo.bacnaoday.service.UserCredentialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
class LoginApiIntegrationTest {

    @Autowired
    WebApplicationContext webApplicationContext;

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserCredentialService userCredentialService;

    MockMvc mockMvc;

    static String readAccessToken(MvcResult result) {
        String json = new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        int key = json.indexOf("\"accessToken\":\"");
        if (key < 0) {
            throw new IllegalStateException("No accessToken in: " + json);
        }
        int from = key + 15;
        int to = json.indexOf('"', from);
        return json.substring(from, to);
    }

    @BeforeEach
    void setUpMockMvcAndSeed() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        userRepository.deleteAll();
        userCredentialService.createUserWithEncodedPassword("apiuser", "password123");
    }

    @Test
    void loginSuccess_returnsUsernameAndJwt() throws Exception {
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"apiuser\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("apiuser"))
                .andExpect(jsonPath("$.accessToken").value(matchesPattern("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$")));
    }

    @Test
    void loginWrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"apiuser\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }

    @Test
    void loginBlankUsername_returns400() throws Exception {
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request"));
    }

    @Test
    void me_withoutToken_returns403_withBearer_returns200() throws Exception {
        mockMvc.perform(get("/api/me")).andExpect(status().isForbidden());

        MvcResult loginResult = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"apiuser\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String token = readAccessToken(loginResult);

        mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("apiuser"));
    }

    @Test
    void logout_returnsOk_withoutBearerMeIs403_tokenStillValidUntilExpiry() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"apiuser\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String token = readAccessToken(loginResult);

        mockMvc.perform(post("/api/logout")).andExpect(status().isOk()).andExpect(jsonPath("$.message").value("Logged out"));

        mockMvc.perform(get("/api/me")).andExpect(status().isForbidden());

        mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("apiuser"));
    }

    @Test
    void logout_withoutLogin_isOkAndIdempotent() throws Exception {
        mockMvc.perform(post("/api/logout")).andExpect(status().isOk()).andExpect(jsonPath("$.message").value("Logged out"));
    }
}
