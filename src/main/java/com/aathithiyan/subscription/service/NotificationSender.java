package com.aathithiyan.subscription.service;

import com.aathithiyan.subscription.entity.NotificationLog;

public interface NotificationSender {
    void sendNotification(NotificationLog notificationLog) throws Exception;
}
