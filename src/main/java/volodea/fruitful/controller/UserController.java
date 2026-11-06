package volodea.fruitful.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import volodea.fruitful.dto.ChangeUsernameRequest;
import volodea.fruitful.dto.UserResponse;
import volodea.fruitful.service.UserService;

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
}
