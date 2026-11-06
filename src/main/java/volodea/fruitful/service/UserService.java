package volodea.fruitful.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import volodea.fruitful.dto.ChangeUsernameRequest;
import volodea.fruitful.dto.UserResponse;
import volodea.fruitful.entity.User;
import volodea.fruitful.exception.UserAlreadyExistException;
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

    public void changeUsername(String email, ChangeUsernameRequest usernameRequest) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        String newUsername = usernameRequest.username().trim();
        if(user.getUsername().equals(newUsername))
            return;

        if(userRepository.existsByUsername(newUsername))
            throw new UserAlreadyExistException();

        user.setUsername(newUsername);
        try {
            userRepository.save(user);
        }catch (DataIntegrityViolationException ex) {
            throw new UserAlreadyExistException();
        }
    }
}
