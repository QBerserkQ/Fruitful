package volodea.fruitful.dto.user;

import volodea.fruitful.entity.Role;

public record UserResponse(
        String username,
        String email,
        Role role
) {
}
