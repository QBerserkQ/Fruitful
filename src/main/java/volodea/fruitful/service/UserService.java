package volodea.fruitful.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import volodea.fruitful.dto.UserResponse;
import volodea.fruitful.entity.User;
import volodea.fruitful.exception.UserNotFoundException;
import volodea.fruitful.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        return new UserResponse(user.getUsername(), user.getEmail(), user.getRole());
    }
}
