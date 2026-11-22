package i_itsm.config;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import i_itsm.model.ITService;
import i_itsm.model.Incident;
import i_itsm.model.Technician;
import i_itsm.model.User;
import i_itsm.repository.ITServiceRepository;
import i_itsm.repository.IncidentRepository;
import i_itsm.repository.TechnicianRepository;
import i_itsm.repository.UserRepository;
import i_itsm.service.FeedbackService;
import i_itsm.service.IncidentService;

/**
 * Sample data so the app can be tried straight away on a new computer (an empty database).
 *
 * Runs once the application has started (after the default users, services, categories and SLA policies exist):
 *  1. No technicians yet  -> adds 3 technicians, each with a login (Ravi Kumar uses the default "technician" login).
 *  2. The "technician" login is not linked to any technician -> links it to the first technician without a login.
 *  3. No incidents yet    -> reports 6 sample incidents through the normal IncidentService, so priority, SLA,
 *                            assignment, duplicate detection, notifications and the audit trail all work as usual,
 *                            and moves some of them along the lifecycle (acknowledged, in progress, resolved, closed).
 *
 * A database that already has technicians and incidents (for example the cloud database) is left as it is.
 * Turn it off with the environment variable SAMPLE_DATA=false.
 */
@Component
public class SampleDataInitializer {

    static final String DEFAULT_TECHNICIAN_LOGIN = "technician";

    private final TechnicianRepository technicianRepository;
    private final UserRepository userRepository;
    private final IncidentRepository incidentRepository;
    private final ITServiceRepository serviceRepository;
    private final IncidentService incidentService;
    private final FeedbackService feedbackService;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;

    public SampleDataInitializer(TechnicianRepository technicianRepository, UserRepository userRepository,
            IncidentRepository incidentRepository, ITServiceRepository serviceRepository, IncidentService incidentService,
            FeedbackService feedbackService, PasswordEncoder passwordEncoder, @Value("${app.sample-data:true}") boolean enabled) {
        this.technicianRepository = technicianRepository;
        this.userRepository = userRepository;
        this.incidentRepository = incidentRepository;
        this.serviceRepository = serviceRepository;
        this.incidentService = incidentService;
        this.feedbackService = feedbackService;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadSampleData() {
        if (!enabled) return;
        try {
            if (technicianRepository.count() == 0) addTechnicians();
            linkDefaultTechnicianLogin();
            if (incidentRepository.count() == 0 && technicianRepository.count() > 0) addIncidents();
        } catch (RuntimeException e) {
            // Sample data is a convenience: a problem here must never stop the application
            System.out.println("Sample data was not loaded: " + e.getMessage());
        }
    }

    // ---- 1. Technicians ----

    void addTechnicians() {
        addTechnician("Ravi Kumar", Set.of("HARDWARE"), DEFAULT_TECHNICIAN_LOGIN, "tech123");
        addTechnician("Priya Sharma", Set.of("NETWORK", "SECURITY"), "priya", "priya123");
        addTechnician("Arjun Mehta", Set.of("SOFTWARE", "DATABASE"), "arjun", "arjun123");
        System.out.println("Sample data: added 3 technicians (logins technician, priya, arjun)");
    }

    private void addTechnician(String name, Set<String> skills, String login, String password) {
        if (userRepository.findByUsername(login).isEmpty())
            userRepository.save(new User(login, passwordEncoder.encode(password), "TECHNICIAN"));
        Technician technician = new Technician(null, name, new HashSet<>(skills), 0);
        technician.setActive(true);
        technician.setUsername(login);
        technicianRepository.save(technician);
    }

    // ---- 2. The default technician login ----

    /** Links the "technician" login to the first technician that has no login yet, if nobody uses it. */
    void linkDefaultTechnicianLogin() {
        if (userRepository.findByUsername(DEFAULT_TECHNICIAN_LOGIN).isEmpty()) return;
        if (technicianRepository.findByUsername(DEFAULT_TECHNICIAN_LOGIN).isPresent()) return;
        technicianRepository.findAll().stream()
                .filter(t -> t.getUsername() == null || t.getUsername().isBlank())
                .min((a, b) -> Long.compare(a.getId(), b.getId()))
                .ifPresent(t -> {
                    t.setUsername(DEFAULT_TECHNICIAN_LOGIN);
                    technicianRepository.save(t);
                    System.out.println("Sample data: linked the 'technician' login to " + t.getName());
                });
    }

    // ---- 3. Incidents ----

    void addIncidents() {
        // A fixed payment problem (closed, with feedback): later payment incidents get it as a suggested fix
        Incident payment = report("Payment application not loading for users",
                "Users cannot complete payments in the payment application. The checkout page shows an error.",
                "SOFTWARE", "Payment Gateway", "HIGH", "MEDIUM", "HIGH", 120);
        asAssignedTechnician(payment, id -> {
            incidentService.acknowledge(id);
            incidentService.updateStatus(id, "IN_PROGRESS");
            incidentService.updateStatus(id, "RESOLVED", "Restarted the payment service and cleared its cache. Card payments work again.");
        });
        as("employee", "EMPLOYEE", () -> feedbackService.submit(payment.getId(), 5, "Fixed quickly, thank you."));
        as("admin", "ADMINISTRATOR", () -> incidentService.updateStatus(payment.getId(), "CLOSED"));

        // Being worked on
        Incident laptop = report("Laptop will not turn on",
                "My laptop does not start even when it is charging. The power light stays off.",
                "HARDWARE", "Employee Portal", "MEDIUM", "MEDIUM", "LOW", 1);
        asAssignedTechnician(laptop, id -> incidentService.updateStatus(id, "IN_PROGRESS"));

        // Acknowledged, not started yet
        Incident wifi = report("Office Wi-Fi keeps disconnecting",
                "The Wi-Fi on the 3rd floor drops every few minutes, so video calls keep failing.",
                "NETWORK", "Corporate Network", "MEDIUM", "HIGH", "MEDIUM", 6);
        asAssignedTechnician(wifi, id -> incidentService.acknowledge(id));

        // Resolved, waiting for the employee's feedback and an admin to close it
        Incident printer = report("Printer offline on floor 2",
                "The shared printer on floor 2 shows as offline for everyone.",
                "HARDWARE", null, "LOW", "LOW", "LOW", 4);
        asAssignedTechnician(printer, id -> {
            incidentService.updateStatus(id, "IN_PROGRESS");
            incidentService.updateStatus(id, "RESOLVED", "Replaced the printer's network cable. It is back online.");
        });

        // New and urgent: the suggested fixes show the closed payment incident
        report("Payment application unavailable",
                "Users cannot complete card payments in the payment application.",
                "SOFTWARE", "Payment Gateway", "HIGH", "HIGH", "HIGH", 300);

        // Reported again by someone else: marked as a possible duplicate of the one above
        report("Payment application unavailable for users",
                "Users cannot complete card payments in the payment application today.",
                "SOFTWARE", "Payment Gateway", "HIGH", "HIGH", "HIGH", 300);

        System.out.println("Sample data: added 6 incidents");
    }

    /** Reports an incident as the default employee, exactly as the Report incident page does. */
    private Incident report(String title, String description, String category, String serviceName,
            String impact, String urgency, String criticality, int affectedUsers) {
        Incident incident = new Incident();
        incident.setTitle(title);
        incident.setDescription(description);
        incident.setCategory(category);
        incident.setImpact(impact);
        incident.setUrgency(urgency);
        incident.setBusinessCriticality(criticality);
        incident.setAffectedUsers(affectedUsers);
        if (serviceName != null) {
            ITService service = serviceRepository.findByNameIgnoreCase(serviceName).orElse(null);
            incident.setAffectedService(service);
        }
        Incident[] created = new Incident[1];
        as("employee", "EMPLOYEE", () -> created[0] = incidentService.createIncident(incident));
        return created[0];
    }

    /** Runs the steps as the technician the incident was assigned to (only that technician may change it). */
    private void asAssignedTechnician(Incident incident, Consumer<Long> steps) {
        Incident current = incidentRepository.findById(incident.getId()).orElse(incident);
        Technician technician = current.getAssignedTechnician();
        if (technician == null || technician.getUsername() == null) return; // nobody to do the work: leave it open
        as(technician.getUsername(), "TECHNICIAN", () -> steps.accept(current.getId()));
    }

    /** Runs an action as if this user were logged in, so permission checks and the audit trail work as usual. */
    private void as(String username, String role, Runnable action) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(username, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
        SecurityContextHolder.setContext(context);
        try {
            action.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
