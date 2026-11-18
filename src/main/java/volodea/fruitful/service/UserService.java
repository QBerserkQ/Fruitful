package volodea.fruitful.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import volodea.fruitful.dto.user.ChangeUsernameRequest;
import volodea.fruitful.dto.user.UserResponse;
import volodea.fruitful.dto.user.UserSummaryResponse;
import volodea.fruitful.entity.User;
import volodea.fruitful.exception.UserAlreadyExistException;
import volodea.fruitful.exception.UserWithEmailNotFoundException;
import volodea.fruitful.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private static final int MIN_QUERY_LENGTH = 3;
    private static final int SEARCH_LIMIT = 10;

    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserWithEmailNotFoundException(email));

        return new UserResponse(user.getUsername(), user.getEmail(), user.getRole());
    }

    public void changeUsername(String email, ChangeUsernameRequest usernameRequest) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserWithEmailNotFoundException(email));

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

    public List<UserSummaryResponse> getUsersByUsername(
            Long currentUserId, String prefix) {
        String savePrefix = prefix.trim();

        if(savePrefix.length() < MIN_QUERY_LENGTH) return List.of();

        Pageable pageable = PageRequest.of(0, SEARCH_LIMIT, Sort.by("username").ascending());

        return userRepository
                .findByUsernameStartingWithIgnoreCaseAndIdNot(savePrefix, currentUserId, pageable)
                .stream()
                .map(user -> new UserSummaryResponse(user.getId(), user.getUsername()))
                .toList();
    }
}
