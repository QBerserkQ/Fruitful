package volodea.fruitful.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import volodea.fruitful.dto.chat.ConversationResponse;
import volodea.fruitful.dto.chat.MessageResponse;
import volodea.fruitful.dto.chat.SendMessageRequest;
import volodea.fruitful.entity.Conversation;
import volodea.fruitful.entity.Message;
import volodea.fruitful.entity.User;
import volodea.fruitful.exception.ConversationNotFoundException;
import volodea.fruitful.exception.SelfConversationNotAllowedException;
import volodea.fruitful.exception.UserNotFoundException;
import volodea.fruitful.repository.ConversationRepository;
import volodea.fruitful.repository.MessageRepository;
import volodea.fruitful.repository.UserRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MessageService messageService;

    // ======================= getOrCreateConversation =======================

    @Test
    void getOrCreateConversation_throws_whenChatWithSelf() {
        assertThatThrownBy(() -> messageService.getOrCreateConversation(1L, 1L))
                .isInstanceOf(SelfConversationNotAllowedException.class);

        verifyNoInteractions(conversationRepository, userRepository, messageRepository);
    }

    @Test
    void getOrCreateConversation_returnsExisting_andOrdersIds() {
        // currentUser = 5, partner = 2: поиск должен идти по паре (2, 5)
        User bob = user(2L, "bob");
        Conversation existing = conversation(10L, bob, user(5L, "eve"));
        when(conversationRepository.findByUserA_IdAndUserB_Id(2L, 5L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(messageRepository.findFirstByConversation_IdOrderByCreatedAtDesc(10L)).thenReturn(Optional.empty());

        ConversationResponse response = messageService.getOrCreateConversation(5L, 2L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.partnerId()).isEqualTo(2L);
        assertThat(response.partnerUsername()).isEqualTo("bob");
        assertThat(response.lastMessage()).isNull();
        assertThat(response.lastMessageAt()).isNull();
        verify(conversationRepository, never()).save(any());
    }

    @Test
    void getOrCreateConversation_createsNew_withCanonicalOrder() {
        User bob = user(2L, "bob");
        User eve = user(5L, "eve");
        when(conversationRepository.findByUserA_IdAndUserB_Id(2L, 5L)).thenReturn(Optional.empty());
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(userRepository.findById(5L)).thenReturn(Optional.of(eve));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });
        when(messageRepository.findFirstByConversation_IdOrderByCreatedAtDesc(10L)).thenReturn(Optional.empty());

        // current = 5, partner = 2, но в БД меньший id должен лечь в userA
        ConversationResponse response = messageService.getOrCreateConversation(5L, 2L);

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(captor.capture());
        assertThat(captor.getValue().getUserA().getId()).isEqualTo(2L);
        assertThat(captor.getValue().getUserB().getId()).isEqualTo(5L);
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.partnerId()).isEqualTo(2L);
    }

    @Test
    void getOrCreateConversation_returnsExisting_whenConcurrentRequestCreatedItFirst() {
        User bob = user(2L, "bob");
        User eve = user(5L, "eve");
        Conversation existing = conversation(10L, bob, eve);
        // первый поиск пустой, после конфликта на UNIQUE второй поиск находит чат
        when(conversationRepository.findByUserA_IdAndUserB_Id(2L, 5L))
                .thenReturn(Optional.empty(), Optional.of(existing));
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(userRepository.findById(5L)).thenReturn(Optional.of(eve));
        when(conversationRepository.save(any(Conversation.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate pair"));
        when(messageRepository.findFirstByConversation_IdOrderByCreatedAtDesc(10L)).thenReturn(Optional.empty());

        ConversationResponse response = messageService.getOrCreateConversation(2L, 5L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.partnerId()).isEqualTo(5L);
    }

    @Test
    void getOrCreateConversation_throws_whenUserDoesNotExist() {
        when(conversationRepository.findByUserA_IdAndUserB_Id(2L, 5L)).thenReturn(Optional.empty());
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.getOrCreateConversation(2L, 5L))
                .isInstanceOf(UserNotFoundException.class);

        verify(conversationRepository, never()).save(any());
    }

    // ============================= sendMessage =============================

    @Test
    void sendMessage_savesTrimmedContent_andReturnsResponse() {
        User alice = user(1L, "alice");
        Conversation conv = conversation(10L, alice, user(2L, "bob"));
        stubSenderAndConversation(alice, conv);
        stubSaveMessage();

        MessageResponse response = messageService.sendMessage(1L, 10L, new SendMessageRequest("  привет  "));

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("привет");
        assertThat(captor.getValue().getSender()).isSameAs(alice);
        assertThat(captor.getValue().getConversation()).isSameAs(conv);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.conversationId()).isEqualTo(10L);
        assertThat(response.senderId()).isEqualTo(1L);
        assertThat(response.content()).isEqualTo("привет");
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    void sendMessage_allowsSecondParticipant() {
        User bob = user(2L, "bob");
        Conversation conv = conversation(10L, user(1L, "alice"), bob);
        stubSenderAndConversation(bob, conv);
        stubSaveMessage();

        MessageResponse response = messageService.sendMessage(2L, 10L, new SendMessageRequest("ответ"));

        assertThat(response.senderId()).isEqualTo(2L);
    }

    @Test
    void sendMessage_throwsNotFound_whenUserIsNotParticipant() {
        User carol = user(3L, "carol");
        Conversation conv = conversation(10L, user(1L, "alice"), user(2L, "bob"));
        stubSenderAndConversation(carol, conv);

        assertThatThrownBy(() -> messageService.sendMessage(3L, 10L, new SendMessageRequest("hi")))
                .isInstanceOf(ConversationNotFoundException.class);

        verify(messageRepository, never()).save(any());
    }

    @Test
    void sendMessage_throwsNotFound_whenConversationDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "alice")));
        when(conversationRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.sendMessage(1L, 10L, new SendMessageRequest("hi")))
                .isInstanceOf(ConversationNotFoundException.class);

        verify(messageRepository, never()).save(any());
    }

    // ============================== getMessages ==============================

    @ParameterizedTest
    @CsvSource({
            "-3, 1000, 0, 50",   // отрицательная страница и огромный size
            "0, 0, 0, 1",        // size меньше минимума
            "2, 20, 2, 20"       // нормальные значения не меняются
    })
    void getMessages_clampsPageAndSize(int page, int size, int expectedPage, int expectedSize) {
        Conversation conv = conversation(10L, user(1L, "alice"), user(2L, "bob"));
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conv));
        when(messageRepository.findByConversation_Id(eq(10L), any(Pageable.class))).thenReturn(List.of());

        messageService.getMessages(1L, 10L, page, size);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(messageRepository).findByConversation_Id(eq(10L), captor.capture());
        Pageable pageable = captor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(expectedPage);
        assertThat(pageable.getPageSize()).isEqualTo(expectedSize);
        assertThat(pageable.getSort().getOrderFor("createdAt").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getMessages_mapsMessagesToResponses() {
        User alice = user(1L, "alice");
        Conversation conv = conversation(10L, alice, user(2L, "bob"));
        OffsetDateTime time = OffsetDateTime.now();
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conv));
        when(messageRepository.findByConversation_Id(eq(10L), any(Pageable.class)))
                .thenReturn(List.of(message(1L, conv, alice, "one", time)));

        List<MessageResponse> result = messageService.getMessages(1L, 10L, 0, 20);

        assertThat(result).hasSize(1);
        MessageResponse first = result.getFirst();
        assertThat(first.id()).isEqualTo(1L);
        assertThat(first.conversationId()).isEqualTo(10L);
        assertThat(first.senderId()).isEqualTo(1L);
        assertThat(first.content()).isEqualTo("one");
        assertThat(first.createdAt()).isEqualTo(time);
    }

    @Test
    void getMessages_throwsNotFound_whenUserIsNotParticipant() {
        Conversation conv = conversation(10L, user(1L, "alice"), user(2L, "bob"));
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conv));

        assertThatThrownBy(() -> messageService.getMessages(3L, 10L, 0, 20))
                .isInstanceOf(ConversationNotFoundException.class);

        verifyNoInteractions(messageRepository);
    }

    // ============================ getConversations ============================

    @Test
    void getConversations_picksPartner_hidesEmptyChats_andSortsByLastMessage() {
        User me = user(2L, "me");
        User anna = user(1L, "anna");
        User carol = user(3L, "carol");
        User dave = user(4L, "dave");
        Conversation withAnna = conversation(11L, anna, me);   // я в роли userB
        Conversation withCarol = conversation(12L, me, carol); // я в роли userA
        Conversation withDave = conversation(13L, me, dave);   // сообщений нет
        OffsetDateTime now = OffsetDateTime.now();

        when(conversationRepository.findAllByUserId(2L)).thenReturn(List.of(withAnna, withCarol, withDave));
        when(messageRepository.findFirstByConversation_IdOrderByCreatedAtDesc(11L))
                .thenReturn(Optional.of(message(1L, withAnna, anna, "old", now.minusHours(2))));
        when(messageRepository.findFirstByConversation_IdOrderByCreatedAtDesc(12L))
                .thenReturn(Optional.of(message(2L, withCarol, me, "new", now.minusHours(1))));
        when(messageRepository.findFirstByConversation_IdOrderByCreatedAtDesc(13L))
                .thenReturn(Optional.empty());

        List<ConversationResponse> result = messageService.getConversations(2L);

        assertThat(result).extracting(ConversationResponse::id).containsExactly(12L, 11L);
        assertThat(result).extracting(ConversationResponse::partnerUsername).containsExactly("carol", "anna");
        assertThat(result).extracting(ConversationResponse::lastMessage).containsExactly("new", "old");
    }

    @Test
    void getConversations_returnsEmptyList_whenUserHasNoChats() {
        when(conversationRepository.findAllByUserId(1L)).thenReturn(List.of());

        assertThat(messageService.getConversations(1L)).isEmpty();
    }

    // =============================== helpers ===============================

    private void stubSenderAndConversation(User sender, Conversation conversation) {
        when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));
    }

    private void stubSaveMessage() {
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(100L);
            m.setCreatedAt(OffsetDateTime.now());
            return m;
        });
    }

    private User user(Long id, String username) {
        return User.builder()
                .id(id)
                .username(username)
                .email(username + "@test.com")
                .passwordHash("hash")
                .build();
    }

    private Conversation conversation(Long id, User userA, User userB) {
        return Conversation.builder()
                .id(id)
                .userA(userA)
                .userB(userB)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    private Message message(Long id, Conversation conversation, User sender, String content, OffsetDateTime createdAt) {
        return Message.builder()
                .id(id)
                .conversation(conversation)
                .sender(sender)
                .content(content)
                .createdAt(createdAt)
                .build();
    }
}