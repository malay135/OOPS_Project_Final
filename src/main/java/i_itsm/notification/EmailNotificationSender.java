package i_itsm.notification;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import i_itsm.model.Incident;
import i_itsm.model.Notification;
import i_itsm.model.User;
import i_itsm.repository.IncidentRepository;
import i_itsm.repository.UserRepository;

/**
 * The EMAIL channel: sends a real email for a notification.
 *
 * Who gets it: the employee who reported the incident, the technician it is assigned to,
 * and the administrators - but only people who saved an email address on the "My account" page.
 * Nobody gets the same email twice.
 *
 * The actual sending is done by BrevoEmailClient. When email is not set up (on our laptops),
 * it prints the email to the console instead.
 */
@Component
public class EmailNotificationSender implements NotificationSender {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final BrevoEmailClient emailClient;
    private final String appUrl;

    public EmailNotificationSender(
            IncidentRepository incidentRepository,
            UserRepository userRepository,
            BrevoEmailClient emailClient,
            @Value("${app.url:http://localhost:5173}") String appUrl) {

        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.emailClient = emailClient;
        this.appUrl = appUrl.trim().replaceAll("/+$", "");
    }

    @Override
    public void send(Notification notification) {

        Incident incident = incidentRepository.findById(notification.getIncidentId()).orElse(null);
        if (incident == null) {
            System.out.println("EMAIL skipped: incident " + notification.getIncidentId() + " not found");
            return;
        }

        Set<String> recipients = recipientsFor(incident);
        if (recipients.isEmpty()) {
            System.out.println("EMAIL skipped for incident #" + notification.getIncidentId()
                    + ": nobody involved has saved an email address");
            return;
        }

        String subject = "[I-ITSM] " + shorten(notification.getMessage(), 120);
        String text = body(notification, incident);

        for (String address : recipients) {
            emailClient.send(address, subject, text);
        }
    }

    /** Reporter, assigned technician's login and administrators that have an email address. Public for the tests. */
    public Set<String> recipientsFor(Incident incident) {

        Set<String> emails = new LinkedHashSet<>();

        if (incident.getReportedBy() != null) {
            add(emails, incident.getReportedBy().getEmail());
        }

        if (incident.getAssignedTechnician() != null && incident.getAssignedTechnician().getUsername() != null) {
            userRepository.findByUsername(incident.getAssignedTechnician().getUsername())
                    .ifPresent(login -> add(emails, login.getEmail()));
        }

        for (User admin : userRepository.findByRole("ADMINISTRATOR")) {
            add(emails, admin.getEmail());
        }

        return emails;
    }

    /** The email text: the message, the incident's details and a link to open it. Public for the tests. */
    public String body(Notification notification, Incident incident) {

        StringBuilder text = new StringBuilder();
        text.append(notification.getMessage()).append("\n\n");
        text.append("Incident #").append(notification.getIncidentId()).append(": ").append(incident.getTitle()).append("\n");
        text.append("Priority: ").append(incident.getPriority())
                .append("   Status: ").append(incident.getStatus()).append("\n");
        text.append("Assigned to: ").append(incident.getAssignedTechnician() == null
                ? "not assigned yet" : incident.getAssignedTechnician().getName()).append("\n");
        if (incident.getSlaDeadline() != null) {
            text.append("SLA deadline: ").append(incident.getSlaDeadline().format(TIME)).append("\n");
        }
        if (incident.getResolutionNotes() != null && !incident.getResolutionNotes().isBlank()) {
            text.append("Resolution notes: ").append(incident.getResolutionNotes()).append("\n");
        }
        text.append("\nOpen it: ").append(appUrl).append("/incidents/").append(notification.getIncidentId()).append("\n\n");
        text.append("You get these emails because your address is saved on the My account page of I-ITSM.\n");
        text.append("To stop them, remove the address there.\n");
        return text.toString();
    }

    private static void add(Set<String> emails, String email) {
        if (email != null && !email.isBlank()) {
            emails.add(email.trim().toLowerCase());
        }
    }

    private static String shorten(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }
}
