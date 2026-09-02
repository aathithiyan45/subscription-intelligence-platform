package com.aathithiyan.subscription.scheduler;

import com.aathithiyan.subscription.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RenewalNotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(RenewalNotificationScheduler.class);

    private final NotificationService notificationService;

    @Value("${app.notification.renewal-window-days:7}")
    private int renewalWindowDays;

    public RenewalNotificationScheduler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "${app.notification.renewal.cron:0 0 8 * * *}")
    public void runDailyRenewalNotificationProcess() {
        log.info("Starting daily renewal notification scan for window of {} days.", renewalWindowDays);
        int created = notificationService.scanAndCreateRenewalNotifications(renewalWindowDays);
        int dispatched = notificationService.dispatchPendingAndRetriableNotifications();
        log.info("Daily renewal notification scan finished. Created: {}, Dispatched/Processed: {}", created, dispatched);
    }
}
