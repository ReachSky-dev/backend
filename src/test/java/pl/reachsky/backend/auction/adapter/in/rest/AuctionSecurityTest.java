package pl.reachsky.backend.auction.adapter.in.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import pl.reachsky.backend.AbstractIntegrationTest;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security tests with the real filter chain — no permissive TestSecurityConfig.
 * Verifies that SELLER-only endpoints reject USER-role callers with HTTP 403.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class AuctionSecurityTest extends AbstractIntegrationTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    JdbcTemplate jdbcTemplate;

    MockMvc mockMvc;
    UUID listingId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        jdbcTemplate.execute("TRUNCATE TABLE auctions CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE listings CASCADE");
        listingId = insertListing();
    }

    @Test
    void createAuction_withUserRoleOnly_returns403() throws Exception {
        mockMvc.perform(post("/api/auctions")
                        .contentType("application/json")
                        .content(dutchRequest())
                        .with(jwt().authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelAuction_withUserRoleOnly_returns403() throws Exception {
        mockMvc.perform(post("/api/auctions/{id}/cancel", UUID.randomUUID())
                        .with(jwt().authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    private UUID insertListing() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO listings (id, seller_id, title, window_starts_at, window_ends_at, capacity, status, created_at)
                VALUES (?, ?, 'Test listing', now(), now() + interval '1 day', 1, 'ACTIVE', now())
                """, id, UUID.randomUUID());
        return id;
    }

    private String dutchRequest() {
        return """
                {
                  "listingId": "%s",
                  "type": "DUTCH",
                  "startsAt": "2028-01-01T12:00:00Z",
                  "endsAt":   "2028-01-02T12:00:00Z",
                  "reservePriceAmount": 3000,
                  "currency": "PLN",
                  "startPriceAmount": 10000,
                  "decrementAmount": 1000,
                  "stepSeconds": 3600,
                  "floorAmount": 3000
                }
                """.formatted(listingId);
    }
}
