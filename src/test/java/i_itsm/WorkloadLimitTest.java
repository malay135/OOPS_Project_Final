package i_itsm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import i_itsm.exception.TechnicianWorkloadExceededException;
import i_itsm.model.Incident;
import i_itsm.model.SystemSetting;
import i_itsm.model.Technician;
import i_itsm.model.User;
import i_itsm.notification.NotificationService;
import i_itsm.repository.DelegationRequestRepository;
import i_itsm.repository.IncidentRepository;
import i_itsm.repository.SystemSettingRepository;
import i_itsm.repository.TechnicianRepository;
import i_itsm.repository.UserRepository;
import i_itsm.service.AssignmentService;
import i_itsm.service.AuditService;
import i_itsm.service.IncidentService;
import i_itsm.service.PriorityService;
import i_itsm.service.SLAService;
import i_itsm.service.SystemSettingService;
import i_itsm.strategy.AssignmentPolicy;
import i_itsm.strategy.LeastLoadedAssignment;
import i_itsm.strategy.SkillBasedAssignment;
import i_itsm.strategy.SkillThenWorkloadAssignment;

/** Technicians at the admin's workload limit get no more incidents, and the reason is clear. */
class WorkloadLimitTest {

    @Mock private TechnicianRepository technicianRepository;
    @Mock private IncidentRepository incidentRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;
    @Mock private UserRepository userRepository;
    @Mock private DelegationRequestRepository delegationRepository;
    @Mock private SystemSettingService settings;
    @Mock private SystemSettingRepository settingRepository;
    @Mock private SLAService slaService;

    private AssignmentService assignment;
    private Technician ravi;   // HARDWARE, at the limit
    private Technician neha;   // SOFTWARE, has room
    private Incident incident;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SkillBasedAssignment skill = new SkillBasedAssignment();
        LeastLoadedAssignment least = new LeastLoadedAssignment();
        assignment = new AssignmentService(skill, least, new SkillThenWorkloadAssignment(skill, least), technicianRepository,
                incidentRepository, notificationService, auditService, userRepository, delegationRepository, settings);
        when(settings.getMaxActiveIncidents()).thenReturn(3);
        when(settings.getAssignmentPolicy()).thenReturn(AssignmentPolicy.SKILL_THEN_WORKLOAD);

        ravi = new Technician(1L, "Ravi Kumar", Set.of("HARDWARE"), 3);
        neha = new Technician(2L, "Neha Iyer", Set.of("SOFTWARE"), 1);
        when(technicianRepository.findByActiveTrue()).thenReturn(List.of(ravi, neha));
        when(technicianRepository.findById(1L)).thenReturn(Optional.of(ravi));
        when(technicianRepository.findById(2L)).thenReturn(Optional.of(neha));

        incident = new Incident();
        incident.setId(7L); incident.setTitle("Docking station broken"); incident.setStatus("OPEN"); incident.setCategory("HARDWARE");
        when(incidentRepository.findById(7L)).thenReturn(Optional.of(incident));
    }

    @Test
    void fullSkilledTechnicianIsSkipped() {
        assertEquals("Neha Iyer", assignment.autoAssignIncident(7L).getName());
        assertEquals(3, ravi.getActiveIncidentCount(), "Ravi gets nothing more");
    }

    @Test
    void everybodyFullGivesTheWorkloadError() {
        neha.setActiveIncidentCount(3);
        TechnicianWorkloadExceededException e = assertThrows(TechnicianWorkloadExceededException.class, () -> assignment.autoAssignIncident(7L));
        assertTrue(e.getMessage().contains("workload limit"));
    }

    @Test
    void skillOnlyExplainsThatTheSkilledTechnicianIsFull() {
        when(settings.getAssignmentPolicy()).thenReturn(AssignmentPolicy.SKILL_ONLY);
        assertThrows(TechnicianWorkloadExceededException.class, () -> assignment.autoAssignIncident(7L));
    }

    @Test
    void adminCannotOverrideToAFullTechnician() {
        TechnicianWorkloadExceededException e = assertThrows(TechnicianWorkloadExceededException.class,
                () -> assignment.overrideAssignment(7L, 1L));
        assertEquals("Ravi Kumar already has 3 active incidents (the limit is 3). Choose another technician or raise the limit in Settings.", e.getMessage());
        assertEquals("Neha Iyer", assignment.overrideAssignment(7L, 2L).getName());
    }

    @Test
    void limitSettingIsChecked() {
        SystemSettingService real = new SystemSettingService(settingRepository);
        when(settingRepository.findById(any())).thenReturn(Optional.empty());
        assertEquals(10, real.getMaxActiveIncidents(), "default limit");
        assertThrows(IllegalArgumentException.class, () -> real.setMaxActiveIncidents(0));
        assertThrows(IllegalArgumentException.class, () -> real.setMaxActiveIncidents(101));
        assertEquals(5, real.setMaxActiveIncidents(5));
        when(settingRepository.findById(any())).thenReturn(Optional.of(new SystemSetting("MAX_ACTIVE_INCIDENTS", "5")));
        assertEquals(5, real.getMaxActiveIncidents());
    }

    @Test
    void adminsAreToldWhenANewIncidentCannotBeAssigned() {
        neha.setActiveIncidentCount(3);
        IncidentService incidents = new IncidentService(incidentRepository, new PriorityService(), auditService, slaService,
                notificationService, technicianRepository, userRepository, assignment, null, null);
        when(incidentRepository.save(any())).thenAnswer(inv -> { Incident i = inv.getArgument(0); if (i.getId() == null) i.setId(7L); return i; });
        when(userRepository.findByRole("ADMINISTRATOR")).thenReturn(List.of(new User("admin", "x", "ADMINISTRATOR")));

        Incident fresh = new Incident();
        fresh.setTitle("Docking station broken"); fresh.setDescription("No display"); fresh.setCategory("HARDWARE");
        fresh.setImpact("LOW"); fresh.setUrgency("LOW"); fresh.setBusinessCriticality("LOW");
        when(incidentRepository.findById(7L)).thenReturn(Optional.of(fresh));
        Incident saved = incidents.createIncident(fresh);

        assertEquals(null, saved.getAssignedTechnician());
        assertEquals(1, saved.getAffectedUsers(), "affected users default to 1");
        verify(auditService).recordAction(eq(7L), eq("ASSIGNMENT_FAILED"), anyString(), eq("system"));
        verify(notificationService).sendNotificationTo(eq(7L), anyString(), eq("IN_APP"), eq("admin"));
    }
}
