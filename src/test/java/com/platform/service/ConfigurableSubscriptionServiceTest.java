package com.platform.service;

import com.platform.config.SubscriptionProperties;
import com.platform.entity.Subscription;
import com.platform.repository.SubscriptionRepository;
import com.platform.repository.AppRepository;
import com.platform.repository.UserRepository;
import com.platform.repository.ApiUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigurableSubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;
    
    @Mock
    private AppRepository appRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private ApiUsageRepository apiUsageRepository;

    private SubscriptionProperties subscriptionProperties;
    private SubscriptionService subscriptionService;

    @BeforeEach
    void setUp() {
        // Setup subscription properties with custom configuration
        subscriptionProperties = new SubscriptionProperties();
        
        // Configure FREE tier with 2 max apps as requested
        SubscriptionProperties.TierConfig freeConfig = new SubscriptionProperties.TierConfig();
        freeConfig.setMaxApps(2);
        freeConfig.setMaxUsers(2);
        freeConfig.setMaxApiRequests(1000L);
        freeConfig.setDescription("Free tier with 2 apps");
        freeConfig.setEnabled(true);
        
        // Configure PRO tier
        SubscriptionProperties.TierConfig proConfig = new SubscriptionProperties.TierConfig();
        proConfig.setMaxApps(10);
        proConfig.setMaxUsers(-1); // Unlimited
        proConfig.setMaxApiRequests(50000L);
        proConfig.setDescription("Pro tier");
        proConfig.setEnabled(true);
        
        subscriptionProperties.getTiers().put("FREE", freeConfig);
        subscriptionProperties.getTiers().put("PRO", proConfig);
        
        subscriptionService = new SubscriptionService(
            subscriptionRepository, 
            appRepository, 
            userRepository, 
            apiUsageRepository, 
            subscriptionProperties
        );
    }

    @Test
    void testCreateDefaultSubscriptionWithConfigurableValues() {
        // Given
        Long corporateId = 1L;
        
        // Mock the repository save method to return the subscription
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription sub = invocation.getArgument(0);
            sub.setId(1L); // Set an ID to simulate database save
            return sub;
        });
        
        // When
        Subscription subscription = subscriptionService.createDefaultSubscription(corporateId);
        
        // Then
        assertNotNull(subscription);
        assertEquals(Subscription.SubscriptionTier.FREE, subscription.getTier());
        assertEquals(2, subscription.getMaxApps()); // Should be 2 as configured
        assertEquals(2, subscription.getMaxUsers());
        assertEquals(1000L, subscription.getMaxApiRequestsPerMonth());
        
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void testGetTierConfiguration() {
        // When
        SubscriptionProperties.TierConfig freeConfig = subscriptionService.getTierConfiguration("FREE");
        SubscriptionProperties.TierConfig proConfig = subscriptionService.getTierConfiguration("PRO");
        
        // Then
        assertNotNull(freeConfig);
        assertEquals(2, freeConfig.getMaxApps());
        assertEquals(2, freeConfig.getMaxUsers());
        assertEquals(1000L, freeConfig.getMaxApiRequests());
        
        assertNotNull(proConfig);
        assertEquals(10, proConfig.getMaxApps());
        assertEquals(-1, proConfig.getMaxUsers()); // Unlimited
        assertEquals(50000L, proConfig.getMaxApiRequests());
    }

    @Test
    void testCreateSubscriptionWithSpecificTier() {
        // Given
        Long corporateId = 1L;
        
        // Mock the repository save method to return the subscription
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription sub = invocation.getArgument(0);
            sub.setId(2L); // Set an ID to simulate database save
            return sub;
        });
        
        // When
        Subscription subscription = subscriptionService.createSubscription(corporateId, Subscription.SubscriptionTier.PRO);
        
        // Then
        assertNotNull(subscription);
        assertEquals(Subscription.SubscriptionTier.PRO, subscription.getTier());
        assertEquals(10, subscription.getMaxApps()); // PRO tier configured value
        assertEquals(-1, subscription.getMaxUsers()); // Unlimited
        assertEquals(50000L, subscription.getMaxApiRequestsPerMonth());
        
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void testGetAllTierConfigurations() {
        // When
        var configurations = subscriptionService.getAllTierConfigurations();
        
        // Then
        assertNotNull(configurations);
        assertEquals(2, configurations.size());
        assertTrue(configurations.containsKey("FREE"));
        assertTrue(configurations.containsKey("PRO"));
        
        assertEquals(2, configurations.get("FREE").getMaxApps());
        assertEquals(10, configurations.get("PRO").getMaxApps());
    }
}