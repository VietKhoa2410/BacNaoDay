package demo.bacnaoday.bootstrap;

import demo.bacnaoday.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(
        properties = {
                "app.user.seed.enabled=true",
                "app.user.seed.username=seeduser",
                "app.user.seed.password=secret"
        })
class UserSeedRunnerTest {

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void seedCreatesUserWithBcryptHash() {
        assertThat(userRepository.existsByUsername("seeduser")).isTrue();
        var user = userRepository.findAll().stream()
                .filter(u -> "seeduser".equals(u.getUsername()))
                .findFirst()
                .orElseThrow();
        assertThat(passwordEncoder.matches("secret", user.getPasswordHash())).isTrue();
    }
}
