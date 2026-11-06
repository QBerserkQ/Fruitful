package volodea.fruitful.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import volodea.fruitful.config.jwt.JwtService;
import volodea.fruitful.dto.*;
import volodea.fruitful.entity.Role;
import volodea.fruitful.entity.User;
import volodea.fruitful.exception.*;
import volodea.fruitful.repository.UserRepository;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse register(RegisterRequest registerRequest, String userAgent) {
        String email = registerRequest.email().trim().toLowerCase(Locale.ROOT);
        String username = registerRequest.username().trim();

        if(userRepository.existsByEmail(email)
                || userRepository.existsByUsername(username))
            throw new UserAlreadyExistException();

        User user = userRepository.save(User.builder()
                .email(email)
                .username(username)
                .passwordHash(passwordEncoder.encode(registerRequest.password()))
                .role(Role.USER).build());

        return createAuthResponse(user, userAgent);
    }

    public AuthResponse login(LoginRequest loginRequest, String userAgent) {
        String email = loginRequest.email().trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if(!passwordEncoder.matches(loginRequest.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return createAuthResponse(user, userAgent);
    }

    @Transactional(noRollbackFor = RefreshTokenExpiredException.class)
    public AuthResponse refresh(RefreshTokenRequest refreshTokenRequest) {
        RotatedSession session = refreshTokenService
                .rotate(refreshTokenRequest.refreshToken());

        User user = session.user();

        return new AuthResponse(
                jwtService.generateJwtToken(user),
                session.refreshToken()
        );
    }

    public void logout(RefreshTokenRequest refreshTokenRequest) {
        refreshTokenService.revoke(refreshTokenRequest.refreshToken());
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest passwordRequest) {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new UserNotFoundException(email)
        );

        Long sessionId = refreshTokenService.getValidSession(user, passwordRequest.refreshToken());

        if(!passwordEncoder.matches(passwordRequest.oldPassword(), user.getPasswordHash())) {
            throw new CurrentPasswordMismatchException();
        }
        if(passwordRequest.oldPassword().equals(passwordRequest.newPassword())) {
            throw new NewPasswordSameAsCurrentException();
        }

        String newPasswordHash = passwordEncoder.encode(passwordRequest.newPassword());
        user.setPasswordHash(newPasswordHash);
        userRepository.save(user);

        refreshTokenService.revokeAllExcept(user, sessionId);
    }

    private AuthResponse createAuthResponse(User user, String userAgent) {
        return new AuthResponse(jwtService.generateJwtToken(user)
                , refreshTokenService.createSession(user, userAgent));
    }
}
