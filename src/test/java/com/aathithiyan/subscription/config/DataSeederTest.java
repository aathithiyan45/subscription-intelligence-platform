package com.aathithiyan.subscription.config;

import com.aathithiyan.subscription.repository.PriceHistoryRepository;
import com.aathithiyan.subscription.repository.SubscriptionRepository;
import com.aathithiyan.subscription.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DataSeederTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(DataSeeder.class)
            .withBean(UserRepository.class, () -> mock(UserRepository.class))
            .withBean(SubscriptionRepository.class, () -> mock(SubscriptionRepository.class))
            .withBean(PriceHistoryRepository.class, () -> mock(PriceHistoryRepository.class))
            .withBean(PasswordEncoder.class, () -> mock(PasswordEncoder.class));

    @Test
    @DisplayName("DataSeeder bean must NOT be loaded when prod profile is active")
    void shouldNotLoadDataSeederInProductionProfile() {
        contextRunner
                .withPropertyValues("spring.profiles.active=prod")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(DataSeeder.class);
                    assertThat(context.containsBean("dataSeeder")).isFalse();
                });
    }

    @Test
    @DisplayName("DataSeeder bean must NOT be loaded when app.seed.enabled is false")
    void shouldNotLoadDataSeederWhenExplicitlyDisabled() {
        contextRunner
                .withPropertyValues("app.seed.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(DataSeeder.class);
                    assertThat(context.containsBean("dataSeeder")).isFalse();
                });
    }

    @Test
    @DisplayName("DataSeeder bean must NOT be loaded in prod even if app.seed.enabled is true")
    void shouldNotLoadDataSeederInProdEvenIfSeedEnabledIsTrue() {
        contextRunner
                .withPropertyValues("spring.profiles.active=prod", "app.seed.enabled=true")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(DataSeeder.class);
                    assertThat(context.containsBean("dataSeeder")).isFalse();
                });
    }

    @Test
    @DisplayName("DataSeeder bean MUST be loaded in development environment")
    void shouldLoadDataSeederInDevelopmentEnvironment() {
        contextRunner
                .withPropertyValues("spring.profiles.active=dev", "app.seed.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(DataSeeder.class);
                    assertThat(context.containsBean("dataSeeder")).isTrue();
                });
    }

    @Test
    @DisplayName("DataSeeder bean MUST be loaded when app.seed.enabled is omitted in non-prod")
    void shouldLoadDataSeederWhenPropertyMissingInNonProd() {
        contextRunner
                .run(context -> {
                    assertThat(context).hasSingleBean(DataSeeder.class);
                    assertThat(context.containsBean("dataSeeder")).isTrue();
                });
    }
}
