package demo.bacnaoday.bootstrap;

import demo.bacnaoday.repository.UserRepository;
import demo.bacnaoday.service.UserCredentialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Order
@ConditionalOnProperty(name = "app.user.seed.enabled", havingValue = "true", matchIfMissing = true)
public class UserSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UserSeedRunner.class);

    private final UserRepository userRepository;
    private final UserCredentialService userCredentialService;
    private final String seedUsername;
    private final String seedPassword;

    public UserSeedRunner(
            UserRepository userRepository,
            UserCredentialService userCredentialService,
            @Value("${app.user.seed.username:demo}") String seedUsername,
            @Value("${app.user.seed.password:}") String seedPassword) {
        this.userRepository = userRepository;
        this.userCredentialService = userCredentialService;
        this.seedUsername = seedUsername;
        this.seedPassword = seedPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(seedPassword)) {
            log.info("User seed skipped: set app.user.seed.password or APP_SEED_PASSWORD to create the initial user");
            return;
        }
        if (userRepository.existsByUsername(seedUsername)) {
            log.debug("User seed skipped: username '{}' already exists", seedUsername);
            return;
        }
        userCredentialService.createUserWithEncodedPassword(seedUsername, seedPassword);
        log.info("Seeded initial user '{}'", seedUsername);
    }
}
