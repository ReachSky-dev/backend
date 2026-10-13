package pl.reachsky.backend.platform;

import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Dołącza classpath:db/seed do lokalizacji Flyway tylko gdy SEED_DEMO_DATA=true.
 * Prod nigdy nie posiada tej zmiennej — seed nie jest wczytywany.
 */
@Configuration
@ConditionalOnProperty(name = "seed-demo-data", havingValue = "true")
class SeedFlywayConfigure implements FlywayConfigurationCustomizer {

    @Override
    public void customize(FluentConfiguration config) {
        var locations = new ArrayList<>(Arrays.asList(config.getLocations()));
        locations.add(new Location("classpath:db/seed"));
        config.locations(locations.toArray(Location[]::new));
    }
}
