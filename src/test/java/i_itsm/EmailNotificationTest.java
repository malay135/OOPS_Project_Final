package i_itsm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sun.net.httpserver.HttpServer;

import i_itsm.model.Incident;
import i_itsm.model.Notification;
import i_itsm.model.Technician;
import i_itsm.model.User;
import i_itsm.notification.BrevoEmailClient;
import i_itsm.notification.EmailNotificationSender;
import i_itsm.repository.IncidentRepository;
import i_itsm.repository.UserRepository;

/** Real email notifications: who gets them, what they say, and the request sent to the email service. */
class EmailNotificationTest {

    @Mock private IncidentRepository incidentRepository;
    @Mock private UserRepository userRepository;
    @Mock private BrevoEmailClient emailClient;

    private EmailNotificationSender sender;
    private Incident incident;
    private User reporter;
    private User techLogin;
    private User admin;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sender = new EmailNotificationSender(incidentRepository, userRepository, emailClient, "https://iitsm.example.app/");

        reporter = new User("riya", "x", "EMPLOYEE");
        reporter.setEmail("riya@example.com");
        techLogin = new User("technician", "x", "TECHNICIAN");
        techLogin.setEmail("ravi@example.com");
        admin = new User("admin", "x", "ADMINISTRATOR");
        admin.setEmail("admin@example.com");
        User adminWithoutEmail = new User("admin2", "x", "ADMINISTRATOR");

        Technician ravi = new Technician();
        ravi.setName("Ravi Kumar");
        ravi.setUsername("technician");

        incident = new Incident();
        incident.setTitle("Laptop will not turn on");
        incident.setPriority("P2");
        incident.setStatus("RESOLVED");
        incident.setSlaDeadline(LocalDateTime.of(2026, 11, 14, 18, 30));
        incident.setResolutionNotes("Replaced the power adapter.");
        incident.setReportedBy(reporter);
        incident.setAssignedTechnician(ravi);

        when(incidentRepository.findById(5L)).thenReturn(Optional.of(incident));
        when(userRepository.findByUsername("technician")).thenReturn(Optional.of(techLogin));
        when(userRepository.findByRole("ADMINISTRATOR")).thenReturn(List.of(admin, adminWithoutEmail));
    }

    @Test
    void reporterTechnicianAndAdminsWithAnEmailGetIt() {
        assertEquals(Set.of("riya@example.com", "ravi@example.com", "admin@example.com"),
                sender.recipientsFor(incident));

        sender.send(new Notification(5L, "Incident 'Laptop will not turn on' status updated to RESOLVED", "EMAIL"));

        verify(emailClient).send(eq("riya@example.com"), anyString(), anyString());
        verify(emailClient).send(eq("ravi@example.com"), anyString(), anyString());
        verify(emailClient).send(eq("admin@example.com"), anyString(), anyString());
        verify(emailClient, times(3)).send(anyString(), anyString(), anyString());
    }

    @Test
    void nobodyWithAnEmailMeansNothingIsSent() {
        reporter.setEmail(null);
        techLogin.setEmail("  ");
        admin.setEmail(null);

        sender.send(new Notification(5L, "Incident created", "EMAIL"));

        verify(emailClient, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void samePersonGetsTheEmailOnlyOnce() {
        admin.setEmail("RIYA@example.com"); // the admin used the reporter's address

        assertEquals(2, sender.recipientsFor(incident).size());
    }

    @Test
    void emailTextHasTheDetailsAndALink() {
        String text = sender.body(new Notification(5L, "Incident resolved", "EMAIL"), incident);

        assertTrue(text.startsWith("Incident resolved"));
        assertTrue(text.contains("Incident #5: Laptop will not turn on"));
        assertTrue(text.contains("Priority: P2   Status: RESOLVED"));
        assertTrue(text.contains("Assigned to: Ravi Kumar"));
        assertTrue(text.contains("SLA deadline: 14 Nov 2026, 18:30"));
        assertTrue(text.contains("Resolution notes: Replaced the power adapter."));
        assertTrue(text.contains("Open it: https://iitsm.example.app/incidents/5"));
    }

    @Test
    void withoutApiKeyTheClientIsNotConfigured() {
        BrevoEmailClient client = new BrevoEmailClient("https://api.brevo.com/v3/smtp/email", "", "", "I-ITSM");
        assertFalse(client.isConfigured());
        client.send("riya@example.com", "Subject", "Text"); // only prints, must not fail
    }

    @Test
    void jsonEscapesQuotesAndNewLines() {
        BrevoEmailClient client = new BrevoEmailClient("https://api.brevo.com/v3/smtp/email", "key", "desk@example.com", "I-ITSM");
        String json = client.toJson("riya@example.com", "Incident 'VPN' \"urgent\"", "Line 1\nLine 2");

        assertEquals("{\"sender\":{\"name\":\"I-ITSM\",\"email\":\"desk@example.com\"},"
                + "\"to\":[{\"email\":\"riya@example.com\"}],"
                + "\"subject\":\"Incident 'VPN' \\\"urgent\\\"\","
                + "\"textContent\":\"Line 1\\nLine 2\"}", json);
    }

    @Test
    void clientPostsToTheEmailApiWithTheKey() throws Exception {
        // A tiny local web server stands in for Brevo and records what it receives
        BlockingQueue<String[]> received = new ArrayBlockingQueue<>(1);
        HttpServer fakeBrevo = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        fakeBrevo.createContext("/v3/smtp/email", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] reply = "{\"messageId\":\"<1@test>\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(201, reply.length);
            exchange.getResponseBody().write(reply);
            exchange.close();
            received.add(new String[] { exchange.getRequestMethod(), exchange.getRequestHeaders().getFirst("api-key"), body });
        });
        fakeBrevo.start();
        try {
            String url = "http://127.0.0.1:" + fakeBrevo.getAddress().getPort() + "/v3/smtp/email";
            BrevoEmailClient client = new BrevoEmailClient(url, "test-key", "desk@example.com", "I-ITSM");

            client.send("riya@example.com", "Hello", "Body text");

            String[] request = received.poll(10, TimeUnit.SECONDS);
            assertEquals("POST", request[0]);
            assertEquals("test-key", request[1]);
            assertTrue(request[2].contains("\"to\":[{\"email\":\"riya@example.com\"}]"));
        } finally {
            fakeBrevo.stop(0);
        }
    }
}
