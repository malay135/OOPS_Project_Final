package i_itsm;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import i_itsm.exception.IncidentAssignmentException;
import i_itsm.model.Incident;
import i_itsm.notification.NotificationService;
import i_itsm.repository.IncidentRepository;
import i_itsm.repository.TechnicianRepository;
import i_itsm.service.AssignmentService;
import i_itsm.service.AuditService;
import i_itsm.strategy.LeastLoadedAssignment;
import i_itsm.strategy.SkillBasedAssignment;

class AssignmentServiceTest {

    @Mock
    private SkillBasedAssignment skillBasedAssignment;

    @Mock
    private LeastLoadedAssignment leastLoadedAssignment;

    @Mock
    private TechnicianRepository technicianRepository;

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditService auditService;

    private AssignmentService assignmentService;

    @BeforeEach
    void setUp() {

        MockitoAnnotations.openMocks(this);

        assignmentService = new AssignmentService(
                skillBasedAssignment,
                leastLoadedAssignment,
                technicianRepository,
                incidentRepository,
                notificationService,
                auditService
        );
    }

    @Test
    void invalidAssignmentStrategyShouldThrowException() {

        Incident incident = new Incident();
        incident.setId(1L);
        incident.setStatus("OPEN");

        when(incidentRepository.findById(1L))
                .thenReturn(Optional.of(incident));

        assertThrows(
                IllegalArgumentException.class,
                () -> assignmentService.assignIncident(
                        1L,
                        "INVALID"
                )
        );
    }

    @Test
    void resolvedIncidentShouldNotBeAssigned() {

        Incident incident = new Incident();
        incident.setId(1L);
        incident.setStatus("RESOLVED");

        when(incidentRepository.findById(1L))
                .thenReturn(Optional.of(incident));

        assertThrows(
                IncidentAssignmentException.class,
                () -> assignmentService.assignIncident(
                        1L,
                        "SKILL"
                )
        );
    }

    @Test
    void closedIncidentShouldNotBeAssigned() {

        Incident incident = new Incident();
        incident.setId(1L);
        incident.setStatus("CLOSED");

        when(incidentRepository.findById(1L))
                .thenReturn(Optional.of(incident));

        assertThrows(
                IncidentAssignmentException.class,
                () -> assignmentService.assignIncident(
                        1L,
                        "LEAST_LOADED"
                )
        );
    }
}