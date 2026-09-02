package com.aathithiyan.subscription.repository;

import com.aathithiyan.subscription.domain.BillingCycle;
import com.aathithiyan.subscription.domain.Role;
import com.aathithiyan.subscription.domain.SubscriptionCategory;
import com.aathithiyan.subscription.domain.SubscriptionStatus;
import com.aathithiyan.subscription.entity.PriceHistory;
import com.aathithiyan.subscription.entity.Subscription;
import com.aathithiyan.subscription.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class RepositoryLayerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PriceHistoryRepository priceHistoryRepository;

    @Test
    void testUserSaveAndFind() {
        String email = "user_" + UUID.randomUUID() + "@example.com";
        User user = User.builder()
                .email(email)
                .password("hashed_password_123")
                .role(Role.USER)
                .build();

        User savedUser = userRepository.save(user);
        assertThat(savedUser.getId()).isNotNull();

        Optional<User> found = userRepository.findByEmail(email);
        assertThat(found).isPresent();
        assertThat(found.get().getRole()).isEqualTo(Role.USER);
        assertThat(userRepository.existsByEmail(email)).isTrue();
    }

    @Test
    void testUniqueEmailConstraint() {
        String email = "dup_" + UUID.randomUUID() + "@example.com";
        User user1 = User.builder()
                .email(email)
                .password("pass1")
                .role(Role.USER)
                .build();
        userRepository.saveAndFlush(user1);

        User user2 = User.builder()
                .email(email)
                .password("pass2")
                .role(Role.USER)
                .build();

        assertThatThrownBy(() -> userRepository.saveAndFlush(user2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void testSubscriptionSaveAndDerivedQueries() {
        String email = "sub_user_" + UUID.randomUUID() + "@example.com";
        User user = userRepository.save(User.builder()
                .email(email)
                .password("pass")
                .role(Role.USER)
                .build());

        Subscription sub1 = subscriptionRepository.save(Subscription.builder()
                .user(user)
                .name("Netflix Premium")
                .price(new BigDecimal("15.99"))
                .billingCycle(BillingCycle.MONTHLY)
                .category(SubscriptionCategory.ENTERTAINMENT)
                .status(SubscriptionStatus.ACTIVE)
                .lastUsedAt(LocalDateTime.now())
                .build());

        Subscription sub2 = subscriptionRepository.save(Subscription.builder()
                .user(user)
                .name("Spotify Duo")
                .price(new BigDecimal("12.99"))
                .billingCycle(BillingCycle.MONTHLY)
                .category(SubscriptionCategory.ENTERTAINMENT)
                .status(SubscriptionStatus.CANCELLED)
                .build());

        assertThat(sub1.getVersion()).isNotNull();

        List<Subscription> activeSubs = subscriptionRepository.findByUserIdAndStatus(user.getId(), SubscriptionStatus.ACTIVE);
        assertThat(activeSubs).hasSize(1);
        assertThat(activeSubs.get(0).getName()).isEqualTo("Netflix Premium");

        List<Subscription> allUserSubs = subscriptionRepository.findByUserId(user.getId());
        assertThat(allUserSubs).hasSize(2);
    }

    @Test
    void testPriceHistoryLog() {
        String email = "price_user_" + UUID.randomUUID() + "@example.com";
        User user = userRepository.save(User.builder()
                .email(email)
                .password("pass")
                .role(Role.USER)
                .build());

        Subscription sub = subscriptionRepository.save(Subscription.builder()
                .user(user)
                .name("Cloud Storage")
                .price(new BigDecimal("9.99"))
                .billingCycle(BillingCycle.MONTHLY)
                .category(SubscriptionCategory.SOFTWARE)
                .status(SubscriptionStatus.ACTIVE)
                .build());

        LocalDateTime now = LocalDateTime.now();
        priceHistoryRepository.save(PriceHistory.builder()
                .subscription(sub)
                .price(new BigDecimal("7.99"))
                .effectiveDate(now.minusMonths(6))
                .build());

        priceHistoryRepository.save(PriceHistory.builder()
                .subscription(sub)
                .price(new BigDecimal("9.99"))
                .effectiveDate(now)
                .build());

        List<PriceHistory> history = priceHistoryRepository.findBySubscriptionIdOrderByEffectiveDateDesc(sub.getId());
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getPrice()).isEqualByComparingTo("9.99");
        assertThat(history.get(1).getPrice()).isEqualByComparingTo("7.99");
    }
}
