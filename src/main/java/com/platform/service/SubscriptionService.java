package com.platform.service;

import com.platform.dto.SubscriptionDTO;
import com.platform.dto.UsageLimitsDTO;
import com.platform.entity.Subscription;
import com.platform.exception.SubscriptionNotFoundException;
import com.platform.repository.ApiUsageRepository;
import com.platform.repository.AppRepository;
import com.platform.repository.SubscriptionRepository;
import com.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final AppRepository appRepository;
    private final UserRepository userRepository;
    private final ApiUsageRepository apiUsageRepository;

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * Get subscription for corporate
     */
    @Transactional(readOnly = true)
    public SubscriptionDTO getSubscription(Long corporateId) {
        log.info("Getting subscription for corporate: {}", corporateId);
        Subscription subscription = subscriptionRepository.findByCorporateId(corporateId)
                .orElseThrow(() -> new SubscriptionNotFoundException(corporateId));
        return SubscriptionDTO.fromEntity(subscription);
    }

    /**
     * Get active subscription for corporate (creates default FREE if not exists)
     */
    @Transactional
    public SubscriptionDTO getActiveSubscription(Long corporateId) {
        log.info("Getting active subscription for corporate: {}", corporateId);

        Subscription subscription = subscriptionRepository.findActiveByCorporateId(corporateId, LocalDateTime.now())
                .orElseGet(() -> {
                    log.warn("No subscription found for corporate: {}, creating default FREE subscription",
                            corporateId);
                    return createDefaultSubscription(corporateId);
                });

        return SubscriptionDTO.fromEntity(subscription);
    }

    /**
     * Create default FREE subscription for corporate
     */
    @Transactional
    public Subscription createDefaultSubscription(Long corporateId) {
        Subscription subscription = new Subscription();
        subscription.setCorporate(new com.platform.entity.Corporate());
        subscription.getCorporate().setId(corporateId);
        subscription.setTier(Subscription.SubscriptionTier.FREE);
        subscription.setMaxApps(1);
        subscription.setMaxUsers(2);
        subscription.setMaxApiRequestsPerMonth(1000L);
        subscription.setFeatures(null); // Features not used yet, set to null
        subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
        subscription.setStartedAt(LocalDateTime.now());

        return subscriptionRepository.save(subscription);
    }

    /**
     * Get usage limits for corporate
     */
    @Transactional(readOnly = true)
    public UsageLimitsDTO getUsageLimits(Long corporateId) {
        log.info("Getting usage limits for corporate: {}", corporateId);

        // Get subscription without creating a default one
        
        Subscription subscription = subscriptionRepository.findActiveByCorporateId(corporateId, LocalDateTime.now())
                .orElseThrow(() -> new SubscriptionNotFoundException(corporateId));

        // Count current usage
        long currentApps = appRepository.countByCorporateIdAndStatus(corporateId,
                com.platform.entity.App.AppStatus.ACTIVE);
        long currentUsers = userRepository.countByCorporateId(corporateId);
        long currentApiRequests = getCurrentMonthApiRequests(corporateId);

        return new UsageLimitsDTO(SubscriptionDTO.fromEntity(subscription), currentApps, currentUsers,
                currentApiRequests);
    }

    /**
     * Check if corporate can create more apps
     */
    @Transactional(readOnly = true)
    public boolean canCreateApp(Long corporateId) {
        log.info("Checking if corporate {} can create more apps", corporateId);

        var subscriptionOpt = subscriptionRepository.findActiveByCorporateId(corporateId, LocalDateTime.now());
        if (subscriptionOpt.isEmpty()) {
            log.warn("No active subscription found for corporate: {}", corporateId);
            return false;
        }

        Subscription subscription = subscriptionOpt.get();

        if (subscription.getMaxApps() == -1) {
            return true;
        }

        long currentApps = appRepository.countByCorporateIdAndStatus(corporateId,
                com.platform.entity.App.AppStatus.ACTIVE);
        return currentApps < subscription.getMaxApps();
    }

    /**
     * Check if corporate can add more users
     */
    @Transactional(readOnly = true)
    public boolean canAddUser(Long corporateId) {
        log.info("Checking if corporate {} can add more users", corporateId);

        var subscriptionOpt = subscriptionRepository.findActiveByCorporateId(corporateId, LocalDateTime.now());
        if (subscriptionOpt.isEmpty()) {
            log.warn("No active subscription found for corporate: {}", corporateId);
            return false;
        }

        Subscription subscription = subscriptionOpt.get();

        if (subscription.getMaxUsers() == -1) {
            return true;
        }

        long currentUsers = userRepository.countByCorporateId(corporateId);
        return currentUsers < subscription.getMaxUsers();
    }

    /**
     * Check if corporate can make more API requests this month
     */
    @Transactional(readOnly = true)
    public boolean canMakeApiRequest(Long corporateId) {
        log.info("Checking if corporate {} can make more API requests", corporateId);

        var subscriptionOpt = subscriptionRepository.findActiveByCorporateId(corporateId, LocalDateTime.now());
        if (subscriptionOpt.isEmpty()) {
            log.warn("No active subscription found for corporate: {}", corporateId);
            return false;
        }

        Subscription subscription = subscriptionOpt.get();

        if (subscription.getMaxApiRequestsPerMonth() == -1) {
            return true;
        }

        long currentRequests = getCurrentMonthApiRequests(corporateId);
        return currentRequests < subscription.getMaxApiRequestsPerMonth();
    }

    /**
     * Track API request
     */
    @Transactional
    public void trackApiRequest(Long corporateId, Long appId) {
        String yearMonth = YearMonth.now().format(YEAR_MONTH_FORMATTER);

        var usage = apiUsageRepository.findByCorporateIdAndYearMonth(corporateId, yearMonth);

        if (usage.isPresent()) {
            // Update existing record
            apiUsageRepository.incrementRequestCount(corporateId, yearMonth);
        } else {
            // Create new record
            var newUsage = new com.platform.entity.ApiUsage();
            newUsage.setCorporateId(corporateId);
            newUsage.setAppId(appId);
            newUsage.setYearMonth(yearMonth);
            newUsage.setRequestCount(1L);
            apiUsageRepository.save(newUsage);
        }
    }

    /**
     * Get current month API requests
     */
    @Transactional(readOnly = true)
    public long getCurrentMonthApiRequests(Long corporateId) {
        String yearMonth = YearMonth.now().format(YEAR_MONTH_FORMATTER);
        return apiUsageRepository.getTotalRequestsForMonth(corporateId, yearMonth);
    }

    /**
     * Upgrade subscription
     */
    @Transactional
    public SubscriptionDTO upgradeSubscription(Long corporateId, Subscription.SubscriptionTier newTier) {
        log.info("Upgrading subscription for corporate {} to {}", corporateId, newTier);

        Subscription subscription = subscriptionRepository.findByCorporateId(corporateId)
                .orElseThrow(() -> new SubscriptionNotFoundException(corporateId));

        // Update tier and limits
        subscription.setTier(newTier);
        subscription.setMaxApps(newTier.getMaxApps());
        subscription.setMaxUsers(newTier.getMaxUsers());
        subscription.setMaxApiRequestsPerMonth(newTier.getMaxApiRequests());

        subscription = subscriptionRepository.save(subscription);
        log.info("Upgraded subscription for corporate {} to {}", corporateId, newTier);

        return SubscriptionDTO.fromEntity(subscription);
    }

    /**
     * Check if subscription is active
     */
    @Transactional(readOnly = true)
    public boolean hasActiveSubscription(Long corporateId) {
        return subscriptionRepository.hasActiveSubscription(corporateId);
    }
}
