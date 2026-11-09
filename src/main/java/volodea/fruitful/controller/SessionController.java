package volodea.fruitful.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import volodea.fruitful.dto.SessionResponse;
import volodea.fruitful.service.RefreshTokenService;
import volodea.fruitful.util.SecurityUtils;

import java.util.List;

@RestController
@RequestMapping("/api/users/me/sessions")
@RequiredArgsConstructor
public class SessionController {
    private final RefreshTokenService refreshTokenService;

    @GetMapping
    public ResponseEntity<List<SessionResponse>> getAllSessions(Authentication authentication) {
        Long sessionId = SecurityUtils.currentSessionId(authentication);
        return ResponseEntity.status(HttpStatus.OK)
                .body(refreshTokenService.getAllSessions(authentication.getName(), sessionId));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long sessionId
    , Authentication authentication) {
        refreshTokenService.deleteSession(authentication.getName(), sessionId);
        return ResponseEntity.noContent().build();
    }
}
