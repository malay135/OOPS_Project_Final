
package i_itsm.notification;

import java.util.List;

import org.springframework.stereotype.Service;

import i_itsm.model.Notification;
import i_itsm.repository.NotificationRepository;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final InAppNotificationSender inAppSender;
    private final SimulatedEmailNotificationSender emailSender;

    public NotificationService(
            NotificationRepository notificationRepository,
            InAppNotificationSender inAppSender,
            SimulatedEmailNotificationSender emailSender) {

        this.notificationRepository = notificationRepository;
        this.inAppSender = inAppSender;
        this.emailSender = emailSender;
    }

    public Notification sendNotification(
            Long incidentId, String message, String channel) {

        Notification notification =
                new Notification(incidentId, message, channel);

        Notification saved =
                notificationRepository.save(notification);

        if ("IN_APP".equalsIgnoreCase(channel)) {
            inAppSender.send(saved);
        }
        else if ("EMAIL".equalsIgnoreCase(channel)) {
            emailSender.send(saved);
        }
        else {
            throw new IllegalArgumentException(
                    "Unsupported notification channel: " + channel);
        }

        return saved;
    }

    public List<Notification> getNotifications(Long incidentId) {
        return notificationRepository
                .findByIncidentIdOrderByCreatedAtDesc(incidentId);
    }

    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    public Notification markAsRead(Long notificationId) {

        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() ->
                    new RuntimeException("Notification not found"));

        notification.setRead(true);

        return notificationRepository.save(notification);
    }
}
