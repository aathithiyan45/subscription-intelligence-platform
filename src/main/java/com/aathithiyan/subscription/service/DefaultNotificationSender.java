package com.aathithiyan.subscription.service;

import com.aathithiyan.subscription.entity.NotificationLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class DefaultNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(DefaultNotificationSender.class);

    private final JavaMailSender mailSender;
    private final String fromEmail;

    public DefaultNotificationSender(
            JavaMailSender mailSender,
            @Value("${app.mail.from:${spring.mail.username:}}") String fromEmail) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    @Override
    public void sendNotification(NotificationLog notificationLog) throws Exception {
        String recipientEmail = notificationLog.getUser().getEmail();
        String subscriptionName = notificationLog.getSubscription().getName();
        String renewalDate = notificationLog.getRenewalDate().toString();

        log.info("Sending renewal email notification [{}] for user [{}] subscription [{}] renewal date [{}]",
                notificationLog.getNotificationType(),
                recipientEmail,
                subscriptionName,
                renewalDate);

        SimpleMailMessage message = new SimpleMailMessage();
        if (fromEmail != null && !fromEmail.isBlank()) {
            message.setFrom(fromEmail);
        }
        message.setTo(recipientEmail);
        message.setSubject("Subscription Renewal Reminder: " + subscriptionName);
        message.setText(String.format(
                "Hello,\n\n" +
                "This is a reminder that your subscription for \"%s\" is scheduled to renew on %s.\n\n" +
                "Please review your subscription details in SubSense if you wish to modify or cancel before renewal.\n\n" +
                "Best regards,\n" +
                "SubSense Team",
                subscriptionName,
                renewalDate
        ));

        mailSender.send(message);
    }
}

