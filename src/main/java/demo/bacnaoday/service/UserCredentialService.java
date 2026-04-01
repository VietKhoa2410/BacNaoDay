package demo.bacnaoday.service;

import demo.bacnaoday.model.User;
import demo.bacnaoday.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Optional;

@Service
public class UserCredentialService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserCredentialService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUserWithEncodedPassword(String username, String rawPassword) {
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("username must not be blank");
        }
        User user = new User();
        user.setUsername(username.trim());
        user.setPasswordHash(encodeRawPassword(rawPassword));
        Instant now = Instant.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return userRepository.save(user);
    }

    public Optional<User> authenticate(String username, String rawPassword) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(rawPassword)) {
            return Optional.empty();
        }
        return userRepository
                .findByUsername(username.trim())
                .filter(user -> passwordEncoder.matches(rawPassword, user.getPasswordHash()));
    }

    private String encodeRawPassword(String rawPassword) {
        if (!StringUtils.hasText(rawPassword)) {
            throw new IllegalArgumentException("password must not be blank");
        }
        return passwordEncoder.encode(rawPassword);
    }
}
