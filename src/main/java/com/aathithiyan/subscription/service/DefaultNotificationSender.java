package com.aathithiyan.subscription.service;

import com.aathithiyan.subscription.entity.NotificationLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DefaultNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(DefaultNotificationSender.class);

    @Override
    public void sendNotification(NotificationLog notificationLog) throws Exception {
        log.info("Sending notification [{}] for user [{}] subscription [{}] renewal date [{}]",
                notificationLog.getNotificationType(),
                notificationLog.getUser().getEmail(),
                notificationLog.getSubscription().getName(),
                notificationLog.getRenewalDate());
    }
}
