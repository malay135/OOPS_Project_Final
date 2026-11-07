package i_itsm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import i_itsm.service.PriorityService;

class PriorityServiceTest {

    private PriorityService priorityService;

    @BeforeEach
    void setUp() {
        priorityService = new PriorityService();
    }

    @Test
    void highImpactHighUrgencyShouldReturnP1() {
        assertEquals(
                "P1",
                priorityService.calculatePriority(
                        "HIGH", "HIGH", "HIGH")
        );
    }

    @Test
    void highImpactMediumUrgencyShouldReturnP2() {
        assertEquals(
                "P2",
                priorityService.calculatePriority(
                        "HIGH", "MEDIUM", "MEDIUM")
        );
    }

    @Test
    void mediumImpactMediumUrgencyShouldReturnP3() {
        assertEquals(
                "P3",
                priorityService.calculatePriority(
                        "MEDIUM", "MEDIUM", "MEDIUM")
        );
    }

    @Test
    void lowImpactLowUrgencyShouldReturnP4() {
        assertEquals(
                "P4",
                priorityService.calculatePriority(
                        "LOW", "LOW", "LOW")
        );
    }

    @Test
    void nullImpactShouldThrowException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> priorityService.calculatePriority(
                        null, "HIGH", "HIGH")
        );
    }

    @Test
    void invalidImpactShouldThrowException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> priorityService.calculatePriority(
                        "INVALID", "HIGH", "HIGH")
        );
    }

    @Test
    void highBusinessCriticalityShouldEscalatePriority() {
        assertEquals(
                "P2",
                priorityService.calculatePriority(
                        "MEDIUM",
                        "MEDIUM",
                        "HIGH")
        );
    }
}