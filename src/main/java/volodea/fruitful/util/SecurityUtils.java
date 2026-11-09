package volodea.fruitful.util;

import org.springframework.security.core.Authentication;
import volodea.fruitful.dto.JwtAuthDetails;

public class SecurityUtils {
    public static Long currentSessionId(Authentication authentication) {
        if (authentication != null && authentication.getDetails() instanceof JwtAuthDetails(Long sessionId)) {
            return sessionId;
        }

        return null;
    }
}
