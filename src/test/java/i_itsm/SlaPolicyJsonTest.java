package i_itsm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import i_itsm.model.SLAPolicy;
import tools.jackson.databind.json.JsonMapper;

/** The Settings page sends SLA times without "priority" or "resolutionTimeHours"; the JSON must still be read. */
class SlaPolicyJsonTest {

    @Test
    void settingsPageBodyIsRead() {
        SLAPolicy p = JsonMapper.builder().build().readValue(
                "{\"responseTimeMinutes\":30,\"resolutionTimeMinutes\":480,\"warningTimeMinutes\":60,\"active\":true}", SLAPolicy.class);
        assertEquals(30, p.getResponseTimeMinutes());
        assertEquals(480, p.getResolutionTimeMinutes());
        assertEquals(60, p.getWarningTimeMinutes());
    }
}
