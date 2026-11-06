package i_itsm.notification;

import org.springframework.stereotype.Component;

import i_itsm.model.Notification;

@Component
public class SimulatedEmailNotificationSender implements NotificationSender {

    @Override
    public void send(Notification notification) {

        System.out.println("SIMULATED EMAIL");
        System.out.println("To: employee@example.com");
        System.out.println("Subject: I-ITSM Incident Notification");
        System.out.println("Message: " + notification.getMessage());
        System.out.println("Email simulated successfully.");
    }
}
