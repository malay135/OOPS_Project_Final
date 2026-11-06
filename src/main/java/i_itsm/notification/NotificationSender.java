package i_itsm.notification;

import i_itsm.model.Notification;

public interface NotificationSender {

    void send(Notification notification);

}
