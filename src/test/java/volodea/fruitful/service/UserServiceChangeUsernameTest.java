package volodea.fruitful.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import volodea.fruitful.dto.ChangeUsernameRequest;
import volodea.fruitful.entity.User;
import volodea.fruitful.exception.UserAlreadyExistException;
import volodea.fruitful.exception.UserNotFoundException;
import volodea.fruitful.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceChangeUsernameTest {

    private static final String EMAIL = "user@mail.com";
    private static final String OLD_USERNAME = "old-name";
    private static final String NEW_USERNAME = "new-name";

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername(OLD_USERNAME);
    }

    @Test
    void changeUsername_whenUsernameIsFree_updatesAndSaves() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername(NEW_USERNAME)).thenReturn(false);

        userService.changeUsername(EMAIL, new ChangeUsernameRequest(NEW_USERNAME));

        assertThat(user.getUsername()).isEqualTo(NEW_USERNAME);
        verify(userRepository).save(user);
    }

    @Test
    void changeUsername_whenUsernameHasSpaces_trimsBeforeCheckAndSave() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername(NEW_USERNAME)).thenReturn(false);

        userService.changeUsername(EMAIL, new ChangeUsernameRequest("  " + NEW_USERNAME + "  "));

        assertThat(user.getUsername()).isEqualTo(NEW_USERNAME);
        verify(userRepository).existsByUsername(NEW_USERNAME);
        verify(userRepository).save(user);
    }

    @Test
    void changeUsername_whenUsernameIsUnchanged_doesNothing() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        userService.changeUsername(EMAIL, new ChangeUsernameRequest(OLD_USERNAME));

        assertThat(user.getUsername()).isEqualTo(OLD_USERNAME);
        verify(userRepository, never()).existsByUsername(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void changeUsername_whenUsernameIsTaken_throwsAndDoesNotSave() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername(NEW_USERNAME)).thenReturn(true);

        assertThatThrownBy(() ->
                userService.changeUsername(EMAIL, new ChangeUsernameRequest(NEW_USERNAME)))
                .isInstanceOf(UserAlreadyExistException.class);

        assertThat(user.getUsername()).isEqualTo(OLD_USERNAME);
        verify(userRepository, never()).save(any());
    }

    @Test
    void changeUsername_whenRaceOnSave_throwsUserAlreadyExist() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername(NEW_USERNAME)).thenReturn(false);
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate username"));

        assertThatThrownBy(() ->
                userService.changeUsername(EMAIL, new ChangeUsernameRequest(NEW_USERNAME)))
                .isInstanceOf(UserAlreadyExistException.class);
    }

    @Test
    void changeUsername_whenUserNotFound_throwsAndChecksNothing() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                userService.changeUsername(EMAIL, new ChangeUsernameRequest(NEW_USERNAME)))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).existsByUsername(any());
        verify(userRepository, never()).save(any());
    }
}