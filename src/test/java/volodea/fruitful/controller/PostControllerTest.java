package volodea.fruitful.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;
import volodea.fruitful.repository.PostRepository;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
class PostControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired
    PostRepository postRepository;

    private String token;

    private String registerAndGetAccessToken() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String body = """
            {"username":"user_%s","email":"%s@test.com","password":"Password123!"}
            """.formatted(unique, unique);

        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(response, "$.accessToken");
    }

    @BeforeEach
    void setUp() throws Exception {
        postRepository.deleteAll();
        token = registerAndGetAccessToken();
    }

    @Test
    void createPost_valid_returns201() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Wake up and start in 90 seconds")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.content").value("Wake up and start in 90 seconds"))
                .andExpect(jsonPath("$.author.username").exists());
    }

    @Test
    void createPost_blankContent_returns400() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("   ")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPost_tooLong_returns400() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("a".repeat(5001))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPost_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("hello")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getFeed_returnsNewestFirst() throws Exception {
        createPost("first");
        createPost("second");

        mockMvc.perform(get("/api/posts").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].content").value("second"))
                .andExpect(jsonPath("$.content[1].content").value("first"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void getFeed_sizeIsClampedTo20() throws Exception {
        mockMvc.perform(get("/api/posts")
                        .param("size", "1000")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void getFeed_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isUnauthorized());
    }

    private void createPost(String content) throws Exception {
        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(content)))
                .andExpect(status().isCreated());
    }

    private String json(String content) {
        return "{\"content\":\"" + content + "\"}";
    }
}