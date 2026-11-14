package volodea.fruitful.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import volodea.fruitful.entity.Conversation;
import volodea.fruitful.entity.Message;
import volodea.fruitful.entity.Role;
import volodea.fruitful.entity.User;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
class ConversationRepositoryTest {

    @Container
    @ServiceConnection
    static org.testcontainers.postgresql.PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");


    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ConversationRepository conversationRepository;
    @Autowired
    private MessageRepository messageRepository;

    // Пользователи создаются по порядку, поэтому alice.id < bob.id < carol.id
    private User alice;
    private User bob;
    private User carol;

    @BeforeEach
    void setUp() {
        alice = createUser("alice");
        bob = createUser("bob");
        carol = createUser("carol");
    }

    @Test
    void findByPair_returnsConversation_whenOrderIsCanonical() {
        Conversation saved = conversationRepository.saveAndFlush(conversation(alice, bob));

        var found = conversationRepository.findByUserA_IdAndUserB_Id(alice.getId(), bob.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    void findByPair_returnsEmpty_whenOrderIsReversed() {
        // Порядок id (min в userA, max в userB) обеспечивает сервис, репозиторий ищет буквально
        conversationRepository.saveAndFlush(conversation(alice, bob));

        var found = conversationRepository.findByUserA_IdAndUserB_Id(bob.getId(), alice.getId());

        assertThat(found).isEmpty();
    }

    @Test
    void save_throws_whenPairAlreadyExists() {
        conversationRepository.saveAndFlush(conversation(alice, bob));

        assertThatThrownBy(() -> conversationRepository.saveAndFlush(conversation(alice, bob)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void save_throws_whenUsersAreInWrongOrder() {
        // CHECK (user_a_id < user_b_id)
        assertThatThrownBy(() -> conversationRepository.saveAndFlush(conversation(bob, alice)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void save_throws_whenChatWithSelf() {
        assertThatThrownBy(() -> conversationRepository.saveAndFlush(conversation(alice, alice)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findAllByUserId_returnsChatsWhereUserIsEitherSide() {
        Conversation ab = conversationRepository.save(conversation(alice, bob));
        Conversation ac = conversationRepository.save(conversation(alice, carol));
        Conversation bc = conversationRepository.saveAndFlush(conversation(bob, carol));

        // bob: userB в (alice, bob) и userA в (bob, carol)
        assertThat(conversationRepository.findAllByUserId(bob.getId()))
                .extracting(Conversation::getId)
                .containsExactlyInAnyOrder(ab.getId(), bc.getId());

        assertThat(conversationRepository.findAllByUserId(alice.getId()))
                .extracting(Conversation::getId)
                .containsExactlyInAnyOrder(ab.getId(), ac.getId());
    }

    @Test
    void findAllByUserId_returnsEmpty_whenUserHasNoChats() {
        conversationRepository.saveAndFlush(conversation(alice, bob));

        assertThat(conversationRepository.findAllByUserId(carol.getId())).isEmpty();
    }

    @Test
    void findByConversationId_returnsNewestFirst_withLimit() {
        Conversation conv = conversationRepository.save(conversation(alice, bob));
        OffsetDateTime base = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1);
        for (int i = 1; i <= 5; i++) {
            // время задаём явно, чтобы порядок не зависел от скорости выполнения
            messageRepository.save(message(conv, i % 2 == 0 ? alice : bob, "msg " + i, base.plusMinutes(i)));
        }
        messageRepository.flush();

        Pageable pageable = PageRequest.of(0, 3, Sort.by("createdAt").descending());
        List<Message> page = messageRepository.findByConversation_Id(conv.getId(), pageable);

        assertThat(page).extracting(Message::getContent)
                .containsExactly("msg 5", "msg 4", "msg 3");
    }

    @Test
    void findByConversationId_returnsOnlyMessagesOfThatConversation() {
        Conversation ab = conversationRepository.save(conversation(alice, bob));
        Conversation ac = conversationRepository.save(conversation(alice, carol));
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        messageRepository.save(message(ab, alice, "to bob", now));
        messageRepository.saveAndFlush(message(ac, alice, "to carol", now));

        List<Message> result = messageRepository.findByConversation_Id(
                ab.getId(), PageRequest.of(0, 10, Sort.by("createdAt").descending()));

        assertThat(result).extracting(Message::getContent).containsExactly("to bob");
    }

    // ---------- helpers ----------

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

    private Message message(Conversation conversation, User sender, String content, OffsetDateTime createdAt) {
        return Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(content)
                .createdAt(createdAt)
                .build();
    }
}