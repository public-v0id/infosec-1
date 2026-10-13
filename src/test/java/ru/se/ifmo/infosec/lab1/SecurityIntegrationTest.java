package ru.se.ifmo.infosec.lab1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import ru.se.ifmo.infosec.lab1.entity.Post;
import ru.se.ifmo.infosec.lab1.entity.User;
import ru.se.ifmo.infosec.lab1.repository.PostRepository;
import ru.se.ifmo.infosec.lab1.repository.UserRepository;
import ru.se.ifmo.infosec.lab1.security.JwtService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void setUp() {
        postRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User(
                "testuser",
                passwordEncoder.encode("testpassword")
        );

        userRepository.save(user);

        token = jwtService.generateToken("testuser");

        postRepository.save(new Post(
                "<script>alert('XSS')</script>",
                "<img src=x onerror=alert('XSS')>"
        ));
    }

    @Test
    void loginWithCorrectPasswordReturnsJwt() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "testuser",
                                  "password": "testpassword"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "testuser",
                                  "password": "wrong"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/data"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithValidTokenReturns200() throws Exception {
        mockMvc.perform(get("/api/data")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user").value("testuser"));
    }

    @Test
    void postsEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postsEndpointWithInvalidTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/posts")
                        .header(
                                "Authorization",
                                "Bearer invalid.token.value"
                        ))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postsEndpointEscapesHtml() throws Exception {
        String response = mockMvc.perform(get("/api/posts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title")
                        .value("&lt;script&gt;alert('XSS')&lt;/script&gt;"))
                .andExpect(jsonPath("$[0].content")
                        .value("&lt;img src=x onerror=alert('XSS')&gt;"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);

        // Проверяем, что ответ — корректный JSON-массив.
        org.junit.jupiter.api.Assertions.assertTrue(json.isArray());
    }

    @Test
    void loginWithUnknownUserReturns401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "username": "unknown-user",
                              "password": "testpassword"
                            }
                            """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithBlankUsernameReturns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "username": " ",
                              "password": "testpassword"
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginWithBlankPasswordReturns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "username": "testuser",
                              "password": ""
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginWithEmptyBodyReturns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginWithSqlInjectionPayloadReturns401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "username": "' OR '1'='1",
                              "password": "anything"
                            }
                            """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithTooLongUsernameReturns400() throws Exception {
        String username = "a".repeat(51);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of(
                                        "username", username,
                                        "password", "testpassword"
                                )
                        )))
                .andExpect(status().isBadRequest());
    }

    @Test
    void databaseStoresPasswordAsBcryptHash() {
        User user = userRepository.findByUsername("testuser")
                .orElseThrow();

        org.junit.jupiter.api.Assertions.assertNotEquals(
                "testpassword",
                user.getPasswordHash()
        );

        org.junit.jupiter.api.Assertions.assertTrue(
                passwordEncoder.matches(
                        "testpassword",
                        user.getPasswordHash()
                )
        );
    }

    @Test
    void protectedEndpointWithAuthorizationHeaderWithoutBearerReturns401()
            throws Exception {
        mockMvc.perform(get("/api/data")
                        .header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithEmptyBearerTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/data")
                        .header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithModifiedJwtReturns401() throws Exception {
        String modifiedToken = token.substring(0, token.length() - 1)
                + (token.endsWith("a") ? "b" : "a");

        mockMvc.perform(get("/api/data")
                        .header("Authorization", "Bearer " + modifiedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithExpiredJwtReturns401() throws Exception {
        String expiredToken = jwtService.generateExpiredToken("testuser");

        mockMvc.perform(get("/api/data")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }
}