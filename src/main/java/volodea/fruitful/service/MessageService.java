package volodea.fruitful.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    public ConversationResponse getOrCreateConversation(Long currentUserId, Long partnerId) {
        Conversation conversation = findOrCreateConversation(currentUserId, partnerId);

        User partner = userRepository.findById(partnerId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + partnerId));

        return toConversationResponse(conversation, partner);
    }

    private ConversationResponse toConversationResponse(Conversation conversation, User partner) {
        Optional<Message> message = messageRepository
                .findFirstByConversation_IdOrderByCreatedAtDesc(conversation.getId());
        return new ConversationResponse(
                conversation.getId()
                , partner.getId()
                , partner.getUsername()
                , message.map(Message::getContent).orElse(null)
                , conversation.getCreatedAt()
                , message.map(Message::getCreatedAt).orElse(null));
    }

    private Conversation findOrCreateConversation(Long currentUserId, Long partnerId) {
        if (currentUserId.equals(partnerId)) throw new SelfConversationNotAllowedException();

        Long userAId = Math.min(currentUserId, partnerId);
        Long userBId = Math.max(currentUserId, partnerId);

        return conversationRepository.findByUserA_IdAndUserB_Id(userAId, userBId)
                .orElseGet(() -> createConversation(userAId, userBId));
    }

    private Conversation createConversation(Long userAId, Long userBId) {
        User userA = userRepository.findById(userAId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userAId));
        User userB = userRepository.findById(userBId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userBId));

        try {
            return conversationRepository.save(
                    Conversation.builder().userA(userA).userB(userB).build()
            );
        } catch (DataIntegrityViolationException e) {
            return conversationRepository.findByUserA_IdAndUserB_Id(userAId, userBId)
                    .orElseThrow(() -> e);
        }
    }

    @Transactional
    public MessageResponse sendMessage(Long currentUserId, Long conversationId, SendMessageRequest request) {
        User sender = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + currentUserId));
        Conversation conversation = getConversationForParticipant(currentUserId, conversationId);

        Message savedMessage = messageRepository.save(Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(request.content().trim())
                .build()
        );

        return  new MessageResponse(savedMessage.getId(), conversation.getId()
                , sender.getId(), savedMessage.getContent(), savedMessage.getCreatedAt());
    }

    private Conversation getConversationForParticipant(Long currentUserId, Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException("Conversation not found: " + conversationId));

        boolean isParticipant = conversation.getUserA().getId().equals(currentUserId)
                || conversation.getUserB().getId().equals(currentUserId);

        if (!isParticipant) {
            throw new ConversationNotFoundException("Conversation not found: " + conversationId);
        }

        return conversation;
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(Long currentUserId, Long conversationId, int page, int size) {
        Conversation conversation = getConversationForParticipant(currentUserId, conversationId);

        int safePage = Math.max(page, 0);
        int safeSize = Math.clamp(size, 1, 50);

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("createdAt").descending());

        return messageRepository.findByConversation_Id(conversation.getId(), pageable)
                .stream()
                .map(this::toMessageResponse)
                .toList();
    }

    private MessageResponse toMessageResponse(Message message) {
        return new MessageResponse(
                message.getId()
                , message.getConversation().getId()
                , message.getSender().getId()
                , message.getContent()
                , message.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversations(Long currentUserId) {
        List<Conversation> conversations = conversationRepository.findAllByUserId(currentUserId);

        return conversations.stream()
                .map(conv -> toConversationResponse(
                        conv,
                        conv.getUserA().getId().equals(currentUserId)
                                ? conv.getUserB()
                                : conv.getUserA()
                ))
                .filter(response -> response.lastMessageAt() != null)
                .sorted(Comparator.comparing(ConversationResponse::lastMessageAt).reversed())
                .toList();
    }
}
