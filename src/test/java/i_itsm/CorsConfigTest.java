package i_itsm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import i_itsm.config.SecurityConfig;

/** The backend accepts requests from the laptop dev server and from the cloud website in APP_URL. */
class CorsConfigTest {

    private CorsConfiguration configFor(String appUrl) {
        return new SecurityConfig().corsConfigurationSource(appUrl)
                .getCorsConfiguration(new MockHttpServletRequest("PUT", "/api/me/email"));
    }

    @Test
    void cloudWebsiteFromAppUrlIsAllowed() {
        CorsConfiguration config = configFor("https://iitsm-bits.netlify.app/");
        assertEquals("https://iitsm-bits.netlify.app", config.checkOrigin("https://iitsm-bits.netlify.app"));
        assertEquals("http://localhost:5173", config.checkOrigin("http://localhost:5173"));
    }

    @Test
    void otherWebsitesAreNotAllowed() {
        assertNull(configFor("https://iitsm-bits.netlify.app").checkOrigin("https://evil.example.com"));
    }
}
