package volodea.fruitful.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import volodea.fruitful.config.jwt.JwtService;
import volodea.fruitful.dto.AuthResponse;
import volodea.fruitful.dto.LoginRequest;
import volodea.fruitful.dto.RefreshTokenRequest;
import volodea.fruitful.dto.RegisterRequest;
import volodea.fruitful.entity.RefreshToken;
import volodea.fruitful.entity.Role;
import volodea.fruitful.entity.User;
import volodea.fruitful.exception.InvalidCredentialsException;
import volodea.fruitful.exception.RefreshTokenExpiredException;
import volodea.fruitful.exception.UserAlreadyExistException;
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
    public AuthResponse register(RegisterRequest registerRequest) {
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

        return createAuthResponse(user);
    }

    public AuthResponse login(LoginRequest loginRequest) {
        String email = loginRequest.email().trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if(!passwordEncoder.matches(loginRequest.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return createAuthResponse(user);
    }

    @Transactional(noRollbackFor = RefreshTokenExpiredException.class)
    public AuthResponse refresh(RefreshTokenRequest refreshTokenRequest) {
        RefreshToken refreshToken = refreshTokenService
                .verifyRefreshToken(refreshTokenRequest.refreshToken());

        User user = refreshToken.getUser();

        return createAuthResponse(user);
    }

    private AuthResponse createAuthResponse(User user) {
        return new AuthResponse(jwtService.generateJwtToken(user)
                , refreshTokenService.generateRefreshToken(user).getToken());
    }
}
