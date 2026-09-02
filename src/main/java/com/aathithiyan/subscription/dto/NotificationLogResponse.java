package com.aathithiyan.subscription.dto;

import com.aathithiyan.subscription.domain.NotificationStatus;
import com.aathithiyan.subscription.domain.NotificationType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class NotificationLogResponse {

    private Long id;
    private Long subscriptionId;
    private String subscriptionName;
    private LocalDate renewalDate;
    private NotificationType notificationType;
    private NotificationStatus status;
    private int retryCount;
    private LocalDateTime sentAt;
    private String errorMessage;

    public NotificationLogResponse() {
    }

    public NotificationLogResponse(Long id, Long subscriptionId, String subscriptionName, LocalDate renewalDate,
                                   NotificationType notificationType, NotificationStatus status,
                                   int retryCount, LocalDateTime sentAt, String errorMessage) {
        this.id = id;
        this.subscriptionId = subscriptionId;
        this.subscriptionName = subscriptionName;
        this.renewalDate = renewalDate;
        this.notificationType = notificationType;
        this.status = status;
        this.retryCount = retryCount;
        this.sentAt = sentAt;
        this.errorMessage = errorMessage;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getSubscriptionName() {
        return subscriptionName;
    }

    public void setSubscriptionName(String subscriptionName) {
        this.subscriptionName = subscriptionName;
    }

    public LocalDate getRenewalDate() {
        return renewalDate;
    }

    public void setRenewalDate(LocalDate renewalDate) {
        this.renewalDate = renewalDate;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
