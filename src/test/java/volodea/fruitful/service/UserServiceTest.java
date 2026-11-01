package volodea.fruitful.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import volodea.fruitful.dto.UserResponse;
import volodea.fruitful.entity.Role;
import volodea.fruitful.entity.User;
import volodea.fruitful.exception.UserNotFoundException;
import volodea.fruitful.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getCurrentUser_existingUser_returnsProfile() {
        User user = User.builder()
                .username("vova").email("vova@example.com").role(Role.USER).build();
        when(userRepository.findByEmail("vova@example.com")).thenReturn(Optional.of(user));

        UserResponse response = userService.getCurrentUser("vova@example.com");

        assertThat(response.username()).isEqualTo("vova");
        assertThat(response.email()).isEqualTo("vova@example.com");
        assertThat(response.role()).isEqualTo(Role.USER);
        verify(userRepository).findByEmail("vova@example.com");
    }

    @Test
    void getCurrentUser_unknownUser_throwsNotFound() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUser("ghost@example.com"))
                .isInstanceOf(UserNotFoundException.class);
    }
}