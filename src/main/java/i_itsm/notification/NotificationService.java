package i_itsm.notification;

import java.util.List;

import org.springframework.stereotype.Service;

import i_itsm.model.Notification;
import i_itsm.repository.NotificationRepository;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSenderFactory senderFactory;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationSenderFactory senderFactory) {

        this.notificationRepository = notificationRepository;
        this.senderFactory = senderFactory;
    }

    public Notification sendNotification(
            Long incidentId, String message, String channel) {

        Notification notification =
                new Notification(incidentId, message, channel);

        Notification saved =
                notificationRepository.save(notification);

        NotificationSender sender =
                senderFactory.getSender(channel);

        sender.send(saved);

        return saved;
    }

    public Notification sendNotificationTo(Long incidentId, String message, String channel, String username) {
        Notification notification = new Notification(incidentId, message, channel);
        notification.setRecipientUsername(username);
        Notification saved = notificationRepository.save(notification);
        senderFactory.getSender(channel).send(saved);
        return saved;
    }

    public List<Notification> getNotifications(Long incidentId) {
        return notificationRepository
                .findByIncidentIdOrderByCreatedAtDesc(incidentId);
    }

    public List<Notification> getAllNotifications() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return List.of();
        return notificationRepository.findByRecipientUsernameOrRecipientUsernameIsNullOrderByCreatedAtDesc(auth.getName());
    }

    public Notification markAsRead(Long notificationId) {

        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() ->
                       new IllegalArgumentException("Notification not found: " + notificationId));

        notification.setRead(true);

        return notificationRepository.save(notification);
    }
}