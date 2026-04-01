package demo.bacnaoday.service;

import demo.bacnaoday.model.User;
import demo.bacnaoday.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCredentialServiceTest {

    @Mock
    UserRepository userRepository;

    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    UserCredentialService service;

    @BeforeEach
    void setUp() {
        service = new UserCredentialService(userRepository, passwordEncoder);
    }

    @Test
    void createUserWithEncodedPassword_storesBcryptHash() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = service.createUserWithEncodedPassword("alice", "secret");

        assertThat(passwordEncoder.matches("secret", saved.getPasswordHash())).isTrue();
        assertThat(saved.getUsername()).isEqualTo("alice");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUserWithEncodedPassword_trimsUsername() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = service.createUserWithEncodedPassword("  bob  ", "x");

        assertThat(saved.getUsername()).isEqualTo("bob");
    }

    @Test
    void createUserWithEncodedPassword_rejectsBlankUsername() {
        assertThatThrownBy(() -> service.createUserWithEncodedPassword(" ", "p"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("username");
    }

    @Test
    void createUserWithEncodedPassword_rejectsBlankPassword() {
        assertThatThrownBy(() -> service.createUserWithEncodedPassword("u", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("password");
    }

    @Test
    void authenticate_returnsUserWhenPasswordMatches() {
        String hash = passwordEncoder.encode("good");
        User stored = userWith("carol", hash);
        when(userRepository.findByUsername("carol")).thenReturn(Optional.of(stored));

        Optional<User> result = service.authenticate("carol", "good");

        assertThat(result).contains(stored);
    }

    @Test
    void authenticate_emptyWhenUserMissing() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThat(service.authenticate("nobody", "any")).isEmpty();
    }

    @Test
    void authenticate_emptyWhenPasswordWrong() {
        User stored = userWith("dave", passwordEncoder.encode("right"));
        when(userRepository.findByUsername("dave")).thenReturn(Optional.of(stored));

        assertThat(service.authenticate("dave", "wrong")).isEmpty();
    }

    @Test
    void authenticate_emptyWhenUsernameOrPasswordBlank() {
        assertThat(service.authenticate("", "p")).isEmpty();
        assertThat(service.authenticate("u", "")).isEmpty();
        assertThat(service.authenticate("  ", "p")).isEmpty();
    }

    @Test
    void authenticate_trimsUsername() {
        User stored = userWith("eve", passwordEncoder.encode("pw"));
        when(userRepository.findByUsername("eve")).thenReturn(Optional.of(stored));

        assertThat(service.authenticate("  eve  ", "pw")).contains(stored);
    }

    private static User userWith(String username, String hash) {
        User u = new User();
        u.setId(1L);
        u.setUsername(username);
        u.setPasswordHash(hash);
        u.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        u.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        return u;
    }
}
