package volodea.fruitful.dto;

import volodea.fruitful.entity.Role;

public record UserResponse(
        String username,
        String email,
        Role role
) {
}
