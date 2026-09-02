package com.aathithiyan.subscription.service;

import com.aathithiyan.subscription.domain.SubscriptionCategory;
import com.aathithiyan.subscription.domain.SubscriptionStatus;
import com.aathithiyan.subscription.dto.PageResponse;
import com.aathithiyan.subscription.dto.SubscriptionCreateRequest;
import com.aathithiyan.subscription.dto.SubscriptionResponse;
import com.aathithiyan.subscription.dto.SubscriptionUpdateRequest;
import com.aathithiyan.subscription.entity.PriceHistory;
import com.aathithiyan.subscription.entity.Subscription;
import com.aathithiyan.subscription.entity.User;
import com.aathithiyan.subscription.exception.ResourceNotFoundException;
import com.aathithiyan.subscription.mapper.SubscriptionMapper;
import com.aathithiyan.subscription.repository.PriceHistoryRepository;
import com.aathithiyan.subscription.repository.SubscriptionRepository;
import com.aathithiyan.subscription.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final SubscriptionMapper subscriptionMapper;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               UserRepository userRepository,
                               PriceHistoryRepository priceHistoryRepository,
                               SubscriptionMapper subscriptionMapper) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.subscriptionMapper = subscriptionMapper;
    }

    @Caching(evict = {
            @CacheEvict(value = "overlaps", key = "#userId"),
            @CacheEvict(value = "renewal_risks", key = "#userId"),
            @CacheEvict(value = "efficiency_scores", key = "#userId"),
            @CacheEvict(value = "optimization_opportunities", key = "#userId"),
            @CacheEvict(value = "spending_analytics", allEntries = true)
    })
    public SubscriptionResponse createSubscription(Long userId, SubscriptionCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Subscription subscription = subscriptionMapper.toEntity(request);
        subscription.setUser(user);
        if (subscription.getStatus() == null) {
            subscription.setStatus(SubscriptionStatus.ACTIVE);
        }

        Subscription savedSubscription = subscriptionRepository.save(subscription);

        // Record initial price history entry
        priceHistoryRepository.save(PriceHistory.builder()
                .subscription(savedSubscription)
                .price(savedSubscription.getPrice())
                .effectiveDate(LocalDateTime.now())
                .build());

        return subscriptionMapper.toResponse(savedSubscription);
    }

    @Caching(evict = {
            @CacheEvict(value = "overlaps", key = "#userId"),
            @CacheEvict(value = "renewal_risks", key = "#userId"),
            @CacheEvict(value = "efficiency_scores", key = "#userId"),
            @CacheEvict(value = "optimization_opportunities", key = "#userId"),
            @CacheEvict(value = "spending_analytics", allEntries = true)
    })
    public SubscriptionResponse updateSubscription(Long userId, Long subscriptionId, SubscriptionUpdateRequest request) {
        Subscription subscription = subscriptionRepository.findByIdAndUserId(subscriptionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", subscriptionId));

        boolean priceChanged = request.getPrice() != null && request.getPrice().compareTo(subscription.getPrice()) != 0;

        subscriptionMapper.updateEntityFromRequest(request, subscription);
        Subscription updatedSubscription = subscriptionRepository.save(subscription);

        if (priceChanged) {
            priceHistoryRepository.save(PriceHistory.builder()
                    .subscription(updatedSubscription)
                    .price(updatedSubscription.getPrice())
                    .effectiveDate(LocalDateTime.now())
                    .build());
        }

        return subscriptionMapper.toResponse(updatedSubscription);
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscription(Long userId, Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findByIdAndUserId(subscriptionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", subscriptionId));

        return subscriptionMapper.toResponse(subscription);
    }

    @Transactional(readOnly = true)
    public PageResponse<SubscriptionResponse> listSubscriptions(Long userId, SubscriptionStatus status,
                                                               SubscriptionCategory category, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }

        Page<Subscription> subscriptionPage;
        if (status != null && category != null) {
            subscriptionPage = subscriptionRepository.findByUserIdAndStatusAndCategory(userId, status, category, pageable);
        } else if (status != null) {
            subscriptionPage = subscriptionRepository.findByUserIdAndStatus(userId, status, pageable);
        } else if (category != null) {
            subscriptionPage = subscriptionRepository.findByUserIdAndCategory(userId, category, pageable);
        } else {
            subscriptionPage = subscriptionRepository.findByUserId(userId, pageable);
        }

        Page<SubscriptionResponse> dtoPage = subscriptionPage.map(subscriptionMapper::toResponse);
        return PageResponse.from(dtoPage);
    }

    @Caching(evict = {
            @CacheEvict(value = "overlaps", key = "#userId"),
            @CacheEvict(value = "renewal_risks", key = "#userId"),
            @CacheEvict(value = "efficiency_scores", key = "#userId"),
            @CacheEvict(value = "optimization_opportunities", key = "#userId"),
            @CacheEvict(value = "spending_analytics", allEntries = true)
    })
    public SubscriptionResponse markSubscriptionUsed(Long userId, Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findByIdAndUserId(subscriptionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", subscriptionId));

        subscription.setLastUsedAt(LocalDateTime.now());
        Subscription updated = subscriptionRepository.save(subscription);
        return subscriptionMapper.toResponse(updated);
    }

    @Caching(evict = {
            @CacheEvict(value = "overlaps", key = "#userId"),
            @CacheEvict(value = "renewal_risks", key = "#userId"),
            @CacheEvict(value = "efficiency_scores", key = "#userId"),
            @CacheEvict(value = "optimization_opportunities", key = "#userId"),
            @CacheEvict(value = "spending_analytics", allEntries = true)
    })
    public void deleteSubscription(Long userId, Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findByIdAndUserId(subscriptionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", subscriptionId));

        List<PriceHistory> history = priceHistoryRepository.findBySubscriptionIdOrderByEffectiveDateDesc(subscriptionId);
        priceHistoryRepository.deleteAll(history);

        subscriptionRepository.delete(subscription);
    }
}
