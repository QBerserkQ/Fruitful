package volodea.fruitful.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import volodea.fruitful.config.security.CustomUserDetails;
import volodea.fruitful.dto.user.ChangeUsernameRequest;
import volodea.fruitful.dto.user.UserResponse;
import volodea.fruitful.dto.user.UserSummaryResponse;
import volodea.fruitful.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return userService.getCurrentUser(authentication.getName());
    }

    @PutMapping("/me/username")
    public ResponseEntity<Void> usernameChange(Authentication authentication
            , @Valid @RequestBody ChangeUsernameRequest usernameRequest) {
        userService.changeUsername(authentication.getName(), usernameRequest);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public List<UserSummaryResponse> searchUser(
            @AuthenticationPrincipal CustomUserDetails details
            , @RequestParam String q) {
        return userService.getUsersByUsername(details.getId(), q);
    }
}
