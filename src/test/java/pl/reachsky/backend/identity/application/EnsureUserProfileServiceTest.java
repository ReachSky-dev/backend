package pl.reachsky.backend.identity.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import pl.reachsky.backend.AbstractIntegrationTest;
import pl.reachsky.backend.identity.application.port.in.EnsureUserProfileUseCase;
import pl.reachsky.backend.identity.domain.UserProfile;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class EnsureUserProfileServiceTest extends AbstractIntegrationTest {

    @Autowired
    EnsureUserProfileUseCase service;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE user_profiles");
    }

    @Test
    void twoCallsWithSameSubjectReturnSameProfile() {
        String sub = UUID.randomUUID().toString();

        UserProfile first = service.ensure(sub, "Test Seller");
        UserProfile second = service.ensure(sub, "Test Seller");

        assertThat(second.getId()).isEqualTo(first.getId());
    }

    @Test
    void sameSubjectCreatesExactlyOneRowInDatabase() {
        String sub = UUID.randomUUID().toString();

        service.ensure(sub, "Test User");
        service.ensure(sub, "Test User");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_profiles WHERE subject = ?",
                Integer.class, sub);
        assertThat(count).isEqualTo(1);
    }
}
