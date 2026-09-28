package pl.reachsky.backend;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Bazowa klasa dla testów integracyjnych.
 *
 * Kontener PostgreSQL uruchamiany jest jako statyczny singleton — raz na cały
 * proces JVM testów, a nie per klasa testowa. Restart kontenera kosztuje 2–4 s
 * narzutu; przy kilkudziesięciu klasach testowych oszczędzamy kilka minut
 * i eliminujemy race conditions przy równoległym wykonaniu.
 *
 * Celowo NIE używamy @Testcontainers + @Container, bo te adnotacje zarządzają
 * cyklem życia per klasa (start przed każdą klasą, stop po każdej klasie).
 * Singleton startuje raz w bloku static i żyje do końca procesu.
 *
 * Spring wykrywa @DynamicPropertySource w hierarchii klas testowych,
 * więc każda podklasa dziedziczy konfigurację datasource wskazującą na ten
 * sam kontener. Spring Context Cache reużywa ten sam kontekst dla klas
 * z identyczną konfiguracją — singleton to umożliwia.
 */
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
