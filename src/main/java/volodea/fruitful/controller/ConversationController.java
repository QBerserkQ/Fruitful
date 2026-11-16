package volodea.fruitful.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import volodea.fruitful.config.security.CustomUserDetails;
import volodea.fruitful.dto.chat.ConversationResponse;
import volodea.fruitful.dto.chat.CreateConversationRequest;
import volodea.fruitful.dto.chat.MessageResponse;
import volodea.fruitful.dto.chat.SendMessageRequest;
import volodea.fruitful.service.MessageService;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {
    private final MessageService messageService;

    @GetMapping
    public List<ConversationResponse> getConversations(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return messageService.getConversations(principal.getId());
    }

    @PostMapping
    public ResponseEntity<ConversationResponse> openConversation
            (@Valid @RequestBody CreateConversationRequest request
                    , @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(messageService.getOrCreateConversation(principal.getId(), request.userId()));
    }

    @GetMapping("/{conversationId}/messages")
    public List<MessageResponse> getMessages(
            @AuthenticationPrincipal CustomUserDetails principal
            , @PathVariable Long conversationId
            , @RequestParam(defaultValue = "0") int page
            , @RequestParam(defaultValue = "20") int size
    ) {

        return messageService.getMessages(principal.getId(), conversationId, page, size);
    }

    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @AuthenticationPrincipal CustomUserDetails principal
            , @PathVariable Long conversationId
            , @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(messageService.sendMessage(principal.getId(), conversationId, request));
    }
}
