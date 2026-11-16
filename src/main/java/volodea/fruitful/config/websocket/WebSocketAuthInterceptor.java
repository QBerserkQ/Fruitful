package volodea.fruitful.config.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import volodea.fruitful.config.jwt.JwtService;
import volodea.fruitful.config.security.CustomUserDetails;
import volodea.fruitful.dto.StompPrincipal;
import volodea.fruitful.exception.MessagingException;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    public @Nullable Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String header = accessor.getFirstNativeHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                throw new MessagingException("Missing or invalid Authorization Bearer");
            }

            String token = header.substring(7);

            try {
                if (!jwtService.validateJwtToken(token)) throw new MessagingException("Invalid token");
                String email = jwtService.getUserEmailFromToken(token);
                CustomUserDetails user = (CustomUserDetails) userDetailsService.loadUserByUsername(email);

                accessor.setUser(new StompPrincipal(String.valueOf(user.getId())));
            } catch (MessagingException e) {
                throw e;
            } catch (Exception e) {
                log.warn("WebSocket auth failed: {}", e.getMessage());
                throw new MessagingException("Invalid token");
            }
        }

        return message;
    }
}
