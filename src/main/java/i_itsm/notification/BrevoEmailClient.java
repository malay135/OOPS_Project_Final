package i_itsm.notification;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Sends one plain-text email through Brevo's email API (over HTTPS).
 *
 * Why an API and not normal SMTP email: free cloud servers (like Render's free plan)
 * block the SMTP ports, but HTTPS always works.
 *
 * Settings come from application.properties / environment variables:
 * BREVO_API_KEY and MAIL_FROM. If they are empty (for example on our laptops),
 * nothing is sent and the email is only printed to the console, like the old simulated sender.
 */
@Component
public class BrevoEmailClient {

    private static final Logger log = LoggerFactory.getLogger(BrevoEmailClient.class);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final URI apiUrl;
    private final String apiKey;
    private final String fromEmail;
    private final String fromName;

    public BrevoEmailClient(
            @Value("${app.mail.api-url:https://api.brevo.com/v3/smtp/email}") String apiUrl,
            @Value("${app.mail.api-key:}") String apiKey,
            @Value("${app.mail.from-email:}") String fromEmail,
            @Value("${app.mail.from-name:I-ITSM Service Desk}") String fromName) {

        this.apiUrl = URI.create(apiUrl.trim());
        this.apiKey = apiKey.trim();
        this.fromEmail = fromEmail.trim();
        this.fromName = fromName.trim();
    }

    /** True when an API key and a sender address are set, so real emails can go out. */
    public boolean isConfigured() {
        return !apiKey.isEmpty() && !fromEmail.isEmpty();
    }

    /**
     * Sends the email in the background, so the user's request never waits for the email service.
     * A failure is written to the log; it never breaks the incident update itself.
     */
    public void send(String to, String subject, String text) {

        if (!isConfigured()) {
            System.out.println("SIMULATED EMAIL (email sending is not set up)");
            System.out.println("To: " + to);
            System.out.println("Subject: " + subject);
            System.out.println(text);
            return;
        }

        HttpRequest request = HttpRequest.newBuilder(apiUrl)
                .timeout(Duration.ofSeconds(20))
                .header("api-key", apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(to, subject, text)))
                .build();

        http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, error) -> {
                    if (error != null) {
                        log.warn("Email to {} failed: {}", to, error.toString());
                    } else if (response.statusCode() >= 300) {
                        log.warn("Email to {} was refused ({}): {}", to, response.statusCode(), response.body());
                    } else {
                        log.info("Email sent to {}: {}", to, subject);
                    }
                });
    }

    /** The request body Brevo expects. Public so the tests can check it. */
    public String toJson(String to, String subject, String text) {
        return "{\"sender\":{\"name\":" + quote(fromName) + ",\"email\":" + quote(fromEmail) + "},"
                + "\"to\":[{\"email\":" + quote(to) + "}],"
                + "\"subject\":" + quote(subject) + ","
                + "\"textContent\":" + quote(text) + "}";
    }

    /** Puts a text value in JSON quotes, escaping the characters JSON does not allow. */
    static String quote(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }
}
