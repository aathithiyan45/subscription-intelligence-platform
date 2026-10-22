package com.aathithiyan.subscription.service;

import com.aathithiyan.subscription.domain.NotificationStatus;
import com.aathithiyan.subscription.domain.NotificationType;
import com.aathithiyan.subscription.entity.NotificationLog;
import com.aathithiyan.subscription.entity.Subscription;
import com.aathithiyan.subscription.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DefaultNotificationSenderTest {

    private JavaMailSender mailSender;
    private DefaultNotificationSender notificationSender;

    private User testUser;
    private Subscription testSubscription;
    private NotificationLog testNotificationLog;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        notificationSender = new DefaultNotificationSender(mailSender, "noreply@subsense.app");

        testUser = new User();
        testUser.setEmail("subscriber@example.com");

        testSubscription = new Subscription();
        testSubscription.setName("Netflix Premium");

        testNotificationLog = NotificationLog.builder()
                .user(testUser)
                .subscription(testSubscription)
                .renewalDate(LocalDate.of(2026, 11, 15))
                .notificationType(NotificationType.RENEWAL_REMINDER)
                .status(NotificationStatus.PENDING)
                .build();
    }

    @Test
    void sendNotification_successSendsSimpleMailMessage() throws Exception {
        notificationSender.sendNotification(testNotificationLog);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getTo()).containsExactly("subscriber@example.com");
        assertThat(sentMessage.getFrom()).isEqualTo("noreply@subsense.app");
        assertThat(sentMessage.getSubject()).isEqualTo("Subscription Renewal Reminder: Netflix Premium");
        assertThat(sentMessage.getText()).contains("Netflix Premium");
        assertThat(sentMessage.getText()).contains("2026-11-15");
        assertThat(sentMessage.getText()).contains("SubSense");
    }

    @Test
    void sendNotification_mailFailurePropagatesException() {
        doThrow(new MailSendException("SMTP server connection timed out"))
                .when(mailSender)
                .send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> notificationSender.sendNotification(testNotificationLog))
                .isInstanceOf(MailSendException.class)
                .hasMessageContaining("SMTP server connection timed out");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendNotification_handlesBlankOrNullFromEmail() throws Exception {
        DefaultNotificationSender senderWithoutFrom = new DefaultNotificationSender(mailSender, "");

        senderWithoutFrom.sendNotification(testNotificationLog);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getTo()).containsExactly("subscriber@example.com");
        assertThat(sentMessage.getFrom()).isNull();
        assertThat(sentMessage.getSubject()).isEqualTo("Subscription Renewal Reminder: Netflix Premium");
    }
}
