package i_itsm.notification;

import org.springframework.stereotype.Component;

@Component
public class NotificationSenderFactory {

    private final InAppNotificationSender inAppSender;
    private final SimulatedEmailNotificationSender emailSender;

    public NotificationSenderFactory(
            InAppNotificationSender inAppSender,
            SimulatedEmailNotificationSender emailSender) {

        this.inAppSender = inAppSender;
        this.emailSender = emailSender;
    }

    public NotificationSender getSender(String channel) {

        if ("IN_APP".equalsIgnoreCase(channel)) {
            return inAppSender;
        }

        if ("EMAIL".equalsIgnoreCase(channel)) {
            return emailSender;
        }

        throw new IllegalArgumentException(
                "Unsupported notification channel: " + channel);
    }
}