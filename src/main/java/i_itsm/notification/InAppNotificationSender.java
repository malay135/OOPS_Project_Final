package i_itsm.notification;

import org.springframework.stereotype.Component;

import i_itsm.model.Notification;

@Component
public class InAppNotificationSender implements NotificationSender {

    @Override
    public void send(Notification notification) {

        System.out.println("IN-APP NOTIFICATION");
        System.out.println("Incident ID: " + notification.getIncidentId());
        System.out.println("Message: " + notification.getMessage());
    }
}
