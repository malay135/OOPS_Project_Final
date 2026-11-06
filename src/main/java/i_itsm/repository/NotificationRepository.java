package i_itsm.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import i_itsm.model.Notification;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification> findByIncidentIdOrderByCreatedAtDesc(
            Long incidentId);
}
