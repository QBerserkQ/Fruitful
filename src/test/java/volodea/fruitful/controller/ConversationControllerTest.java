package volodea.fruitful.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import volodea.fruitful.config.security.CustomUserDetails;
import volodea.fruitful.entity.Conversation;
import volodea.fruitful.entity.Message;
import volodea.fruitful.entity.Role;
import volodea.fruitful.entity.User;
import volodea.fruitful.repository.ConversationRepository;
import volodea.fruitful.repository.MessageRepository;
import volodea.fruitful.repository.UserRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Проверяет цепочку целиком: security-правила, разбор @AuthenticationPrincipal,
 * валидацию, сервис и реальную БД.
 * JWT-фильтр здесь обходится: принципал подставляется напрямую через authentication(...).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
class ConversationControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");


    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ConversationRepository conversationRepository;
    @Autowired
    private MessageRepository messageRepository;

    private User alice;
    private User bob;
    private User carol;

    @BeforeEach
    void setUp() {
        alice = createUser("alice");
        bob = createUser("bob");
        carol = createUser("carol");
    }

    // ============================ безопасность ============================

    @Test
    void allEndpoints_return401_withoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/conversations")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/conversations")
                        .contentType(MediaType.APPLICATION_JSON).content(openBody(bob.getId())))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/conversations/1/messages")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/conversations/1/messages")
                        .contentType(MediaType.APPLICATION_JSON).content(sendBody("hi")))
                .andExpect(status().isUnauthorized());
    }

    // ======================== POST /api/conversations ========================

    @Test
    void openConversation_createsChat_andReturnsPartnerInfo() throws Exception {
        mockMvc.perform(post("/api/conversations")
                        .with(asUser(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(openBody(bob.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.partnerId").value(bob.getId().intValue()))
                .andExpect(jsonPath("$.partnerUsername").value("bob"));

        assertThat(conversationRepository.count()).isEqualTo(1);
    }

    @Test
    void openConversation_isIdempotent_fromBothSides() throws Exception {
        mockMvc.perform(post("/api/conversations").with(asUser(alice))
                        .contentType(MediaType.APPLICATION_JSON).content(openBody(bob.getId())))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/conversations").with(asUser(alice))
                        .contentType(MediaType.APPLICATION_JSON).content(openBody(bob.getId())))
                .andExpect(status().isOk());
        // с другой стороны это тот же самый чат, партнёром становится alice
        mockMvc.perform(post("/api/conversations").with(asUser(bob))
                        .contentType(MediaType.APPLICATION_JSON).content(openBody(alice.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partnerUsername").value("alice"));

        assertThat(conversationRepository.count()).isEqualTo(1);
    }

    @Test
    void openConversation_returns400_forChatWithSelf() throws Exception {
        mockMvc.perform(post("/api/conversations").with(asUser(alice))
                        .contentType(MediaType.APPLICATION_JSON).content(openBody(alice.getId())))
                .andExpect(status().isBadRequest());

        assertThat(conversationRepository.count()).isZero();
    }

    @Test
    void openConversation_returns404_forUnknownUser() throws Exception {
        mockMvc.perform(post("/api/conversations").with(asUser(alice))
                        .contentType(MediaType.APPLICATION_JSON).content(openBody(999_999L)))
                .andExpect(status().isNotFound());
    }

    @Test
    void openConversation_returns400_forInvalidBody() throws Exception {
        mockMvc.perform(post("/api/conversations").with(asUser(alice))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/conversations").with(asUser(alice))
                        .contentType(MediaType.APPLICATION_JSON).content(openBody(-1L)))
                .andExpect(status().isBadRequest());
    }

    // ================= POST /api/conversations/{id}/messages =================

    @Test
    void sendMessage_returns201_andTrimsContent() throws Exception {
        Conversation conv = conversationRepository.saveAndFlush(conversation(alice, bob));

        mockMvc.perform(post("/api/conversations/{id}/messages", conv.getId())
                        .with(asUser(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sendBody("  привет  ")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("привет"))
                .andExpect(jsonPath("$.senderId").value(alice.getId().intValue()))
                .andExpect(jsonPath("$.conversationId").value(conv.getId().intValue()));

        assertThat(messageRepository.count()).isEqualTo(1);
    }

    @Test
    void sendMessage_returns400_forBlankOrTooLongContent() throws Exception {
        Conversation conv = conversationRepository.saveAndFlush(conversation(alice, bob));

        mockMvc.perform(post("/api/conversations/{id}/messages", conv.getId()).with(asUser(alice))
                        .contentType(MediaType.APPLICATION_JSON).content(sendBody("   ")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/conversations/{id}/messages", conv.getId()).with(asUser(alice))
                        .contentType(MediaType.APPLICATION_JSON).content(sendBody("a".repeat(2001))))
                .andExpect(status().isBadRequest());

        assertThat(messageRepository.count()).isZero();
    }

    @Test
    void sendMessage_returns404_forNonParticipant() throws Exception {
        Conversation conv = conversationRepository.saveAndFlush(conversation(alice, bob));

        mockMvc.perform(post("/api/conversations/{id}/messages", conv.getId())
                        .with(asUser(carol))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sendBody("я чужой")))
                .andExpect(status().isNotFound());

        assertThat(messageRepository.count()).isZero();
    }

    // ================= GET /api/conversations/{id}/messages =================

    @Test
    void getMessages_returnsNewestFirst_andSupportsPaging() throws Exception {
        Conversation conv = conversationRepository.saveAndFlush(conversation(alice, bob));
        OffsetDateTime base = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1);
        seedMessage(conv, alice, "m1", base.plusMinutes(1));
        seedMessage(conv, bob, "m2", base.plusMinutes(2));
        seedMessage(conv, alice, "m3", base.plusMinutes(3));

        // участник в роли userB тоже читает историю
        mockMvc.perform(get("/api/conversations/{id}/messages", conv.getId()).with(asUser(bob)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].content").value("m3"))
                .andExpect(jsonPath("$[2].content").value("m1"));

        mockMvc.perform(get("/api/conversations/{id}/messages", conv.getId())
                        .param("page", "0").param("size", "2").with(asUser(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/conversations/{id}/messages", conv.getId())
                        .param("page", "1").param("size", "2").with(asUser(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].content").value("m1"));
    }

    @Test
    void getMessages_returns404_forNonParticipant() throws Exception {
        Conversation conv = conversationRepository.saveAndFlush(conversation(alice, bob));
        seedMessage(conv, alice, "секрет", OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/conversations/{id}/messages", conv.getId()).with(asUser(carol)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMessages_returns404_forUnknownConversation() throws Exception {
        mockMvc.perform(get("/api/conversations/{id}/messages", 999_999L).with(asUser(alice)))
                .andExpect(status().isNotFound());
    }

    // ======================== GET /api/conversations ========================

    @Test
    void getConversations_sortedByLastMessage_newestFirst() throws Exception {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Conversation withBob = conversationRepository.saveAndFlush(conversation(alice, bob));
        Conversation withCarol = conversationRepository.saveAndFlush(conversation(alice, carol));
        seedMessage(withBob, bob, "старое", now.minusHours(2));
        seedMessage(withCarol, carol, "новое", now.minusHours(1));

        mockMvc.perform(get("/api/conversations").with(asUser(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].partnerUsername").value("carol"))
                .andExpect(jsonPath("$[0].lastMessage").value("новое"))
                .andExpect(jsonPath("$[1].partnerUsername").value("bob"))
                .andExpect(jsonPath("$[1].lastMessage").value("старое"));
    }

    @Test
    void getConversations_showsEmptyChats_andOnlyOwnChats() throws Exception {
        Conversation ab = conversationRepository.saveAndFlush(conversation(alice, bob));
        conversationRepository.saveAndFlush(conversation(bob, carol)); // пустой чат
        seedMessage(ab, alice, "привет", OffsetDateTime.now(ZoneOffset.UTC));

        // у bob два чата: с alice (есть сообщение) и с carol (пустой). Пустой больше не скрывается
        mockMvc.perform(get("/api/conversations").with(asUser(bob)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].partnerUsername", containsInAnyOrder("alice", "carol")));

        // у carol один чат: пустой, с bob
        mockMvc.perform(get("/api/conversations").with(asUser(carol)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].partnerUsername").value("bob"));
    }

    // =============================== helpers ===============================

    private RequestPostProcessor asUser(User user) {
        CustomUserDetails details = new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        return authentication(new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    private User createUser(String name) {
        return userRepository.saveAndFlush(User.builder()
                .username(name)
                .email(name + "@test.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build());
    }

    private Conversation conversation(User a, User b) {
        return Conversation.builder().userA(a).userB(b).build();
    }

    private void seedMessage(Conversation conversation, User sender, String content, OffsetDateTime createdAt) {
        messageRepository.saveAndFlush(Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(content)
                .createdAt(createdAt)
                .build());
    }

    private String openBody(Long userId) {
        return "{\"userId\": " + userId + "}";
    }

    private String sendBody(String content) {
        return "{\"content\": \"" + content + "\"}";
    }
}