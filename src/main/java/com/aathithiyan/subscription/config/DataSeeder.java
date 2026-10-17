package com.aathithiyan.subscription.config;

import com.aathithiyan.subscription.domain.BillingCycle;
import com.aathithiyan.subscription.domain.Role;
import com.aathithiyan.subscription.domain.SubscriptionCategory;
import com.aathithiyan.subscription.domain.SubscriptionStatus;
import com.aathithiyan.subscription.entity.PriceHistory;
import com.aathithiyan.subscription.entity.Subscription;
import com.aathithiyan.subscription.entity.User;
import com.aathithiyan.subscription.repository.PriceHistoryRepository;
import com.aathithiyan.subscription.repository.SubscriptionRepository;
import com.aathithiyan.subscription.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@Profile("!prod")
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      SubscriptionRepository subscriptionRepository,
                      PriceHistoryRepository priceHistoryRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        User user = userRepository.findByEmail("test@example.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("test@example.com")
                        .password(passwordEncoder.encode("password123"))
                        .role(Role.USER)
                        .build()));

        if (subscriptionRepository.findByUserId(user.getId()).isEmpty()) {
            LocalDateTime now = LocalDateTime.now();

            // 1. Netflix Premium (Triggers Renewal Price Hike Risk & Category Overlap)
            Subscription netflix = subscriptionRepository.save(Subscription.builder()
                    .user(user)
                    .name("Netflix Premium")
                    .price(new BigDecimal("22.99"))
                    .billingCycle(BillingCycle.MONTHLY)
                    .category(SubscriptionCategory.ENTERTAINMENT)
                    .status(SubscriptionStatus.ACTIVE)
                    .lastUsedAt(now.minusDays(1))
                    .build());

            priceHistoryRepository.save(PriceHistory.builder()
                    .subscription(netflix)
                    .price(new BigDecimal("15.99"))
                    .effectiveDate(now.minusMonths(6))
                    .build());

            priceHistoryRepository.save(PriceHistory.builder()
                    .subscription(netflix)
                    .price(new BigDecimal("19.99"))
                    .effectiveDate(now.minusMonths(3))
                    .build());

            priceHistoryRepository.save(PriceHistory.builder()
                    .subscription(netflix)
                    .price(new BigDecimal("22.99"))
                    .effectiveDate(now.minusDays(10))
                    .build());

            // 2. Disney+ Bundle (Triggers Category Overlap with Netflix in ENTERTAINMENT)
            subscriptionRepository.save(Subscription.builder()
                    .user(user)
                    .name("Disney+ Bundle")
                    .price(new BigDecimal("14.99"))
                    .billingCycle(BillingCycle.MONTHLY)
                    .category(SubscriptionCategory.ENTERTAINMENT)
                    .status(SubscriptionStatus.ACTIVE)
                    .lastUsedAt(now.minusDays(2))
                    .build());

            // 3. Equinox Gym (Triggers Underutilized / 45-day Inactivity Alert)
            subscriptionRepository.save(Subscription.builder()
                    .user(user)
                    .name("Equinox Gym")
                    .price(new BigDecimal("89.00"))
                    .billingCycle(BillingCycle.MONTHLY)
                    .category(SubscriptionCategory.FITNESS)
                    .status(SubscriptionStatus.ACTIVE)
                    .lastUsedAt(now.minusDays(45))
                    .build());

            // 4. AWS Cloud Services (Triggers High Monthly Spend Optimization Opportunity)
            subscriptionRepository.save(Subscription.builder()
                    .user(user)
                    .name("AWS Cloud Services")
                    .price(new BigDecimal("149.99"))
                    .billingCycle(BillingCycle.MONTHLY)
                    .category(SubscriptionCategory.SOFTWARE)
                    .status(SubscriptionStatus.ACTIVE)
                    .lastUsedAt(now)
                    .build());

            // 5. Spotify Family
            subscriptionRepository.save(Subscription.builder()
                    .user(user)
                    .name("Spotify Family")
                    .price(new BigDecimal("16.99"))
                    .billingCycle(BillingCycle.MONTHLY)
                    .category(SubscriptionCategory.ENTERTAINMENT)
                    .status(SubscriptionStatus.ACTIVE)
                    .lastUsedAt(now.minusDays(3))
                    .build());

            // 6. ChatGPT Plus
            subscriptionRepository.save(Subscription.builder()
                    .user(user)
                    .name("ChatGPT Plus")
                    .price(new BigDecimal("20.00"))
                    .billingCycle(BillingCycle.MONTHLY)
                    .category(SubscriptionCategory.WORK)
                    .status(SubscriptionStatus.ACTIVE)
                    .lastUsedAt(now.minusDays(1))
                    .build());
        }
    }
}
