package i_itsm.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import i_itsm.model.Notification;
import i_itsm.notification.NotificationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // Get all notifications
    @GetMapping
    public List<Notification> getAllNotifications() {
        return notificationService.getAllNotifications();
    }

    // Get notifications for a particular incident
    @GetMapping("/{incidentId}")
    public List<Notification> getNotifications(
            @PathVariable Long incidentId) {

        return notificationService.getNotifications(incidentId);
    }

    // Create and send a notification
    @PostMapping
    public Notification sendNotification(
            @RequestParam Long incidentId,
            @RequestParam String message,
            @RequestParam String channel) {

        return notificationService.sendNotification(
                incidentId, message, channel);
    }

    // Mark a notification as read
    @PutMapping("/{notificationId}/read")
    public Notification markAsRead(
            @PathVariable Long notificationId) {

        return notificationService.markAsRead(notificationId);
    }
}
