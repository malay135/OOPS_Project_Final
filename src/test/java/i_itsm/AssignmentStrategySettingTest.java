package i_itsm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import i_itsm.exception.IncidentAssignmentException;
import i_itsm.model.Incident;
import i_itsm.model.SystemSetting;
import i_itsm.model.Technician;
import i_itsm.notification.NotificationService;
import i_itsm.repository.DelegationRequestRepository;
import i_itsm.repository.IncidentRepository;
import i_itsm.repository.SystemSettingRepository;
import i_itsm.repository.TechnicianRepository;
import i_itsm.repository.UserRepository;
import i_itsm.service.AssignmentService;
import i_itsm.service.AuditService;
import i_itsm.service.SystemSettingService;
import i_itsm.strategy.AssignmentPolicy;
import i_itsm.strategy.LeastLoadedAssignment;
import i_itsm.strategy.SkillBasedAssignment;
import i_itsm.strategy.SkillThenWorkloadAssignment;

/** The admin's chosen assignment strategy decides who gets a new incident. */
class AssignmentStrategySettingTest {

    @Mock private TechnicianRepository technicianRepository;
    @Mock private IncidentRepository incidentRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;
    @Mock private UserRepository userRepository;
    @Mock private DelegationRequestRepository delegationRepository;
    @Mock private SystemSettingService settings;
    @Mock private SystemSettingRepository settingRepository;

    private AssignmentService service;
    private Incident incident;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SkillBasedAssignment skill = new SkillBasedAssignment();
        LeastLoadedAssignment least = new LeastLoadedAssignment();
        service = new AssignmentService(skill, least, new SkillThenWorkloadAssignment(skill, least), technicianRepository,
                incidentRepository, notificationService, auditService, userRepository, delegationRepository, settings);

        Technician asha = new Technician(1L, "Asha Rao", Set.of("NETWORK"), 2);
        Technician ravi = new Technician(2L, "Ravi Kumar", Set.of("HARDWARE"), 5);
        Technician neha = new Technician(3L, "Neha Iyer", Set.of("SOFTWARE"), 0);
        when(technicianRepository.findByActiveTrue()).thenReturn(List.of(asha, ravi, neha));

        incident = new Incident();
        incident.setId(9L); incident.setTitle("Laptop will not turn on"); incident.setStatus("OPEN"); incident.setCategory("HARDWARE");
        when(incidentRepository.findById(9L)).thenReturn(Optional.of(incident));
    }

    private Technician assignWith(AssignmentPolicy policy) {
        when(settings.getAssignmentPolicy()).thenReturn(policy);
        return service.autoAssignIncident(9L);
    }

    @Test
    void skillThenWorkloadPicksTheSkilledTechnician() {
        assertEquals("Ravi Kumar", assignWith(AssignmentPolicy.SKILL_THEN_WORKLOAD).getName());
        verify(auditService).recordAction(eq(9L), eq("AUTOMATIC_SKILL_THEN_WORKLOAD"), anyString(), any());
    }

    @Test
    void leastLoadedIgnoresSkills() {
        assertEquals("Neha Iyer", assignWith(AssignmentPolicy.LEAST_LOADED).getName());
        verify(auditService).recordAction(eq(9L), eq("AUTOMATIC_LEAST_LOADED"), anyString(), any());
    }

    @Test
    void skillOnlyLeavesTheIncidentUnassignedWhenNobodyHasTheSkill() {
        incident.setCategory("CLOUD");
        assertThrows(IncidentAssignmentException.class, () -> assignWith(AssignmentPolicy.SKILL_ONLY));
    }

    @Test
    void skillThenWorkloadFallsBackWhenNobodyHasTheSkill() {
        incident.setCategory("CLOUD");
        assertEquals("Neha Iyer", assignWith(AssignmentPolicy.SKILL_THEN_WORKLOAD).getName());
    }

    @Test
    void settingDefaultsToSkillThenWorkload() {
        when(settingRepository.findById(any())).thenReturn(Optional.empty());
        assertEquals(AssignmentPolicy.SKILL_THEN_WORKLOAD, new SystemSettingService(settingRepository).getAssignmentPolicy());
    }

    @Test
    void savedSettingIsReadBack() {
        when(settingRepository.findById(any())).thenReturn(Optional.of(new SystemSetting("ASSIGNMENT_STRATEGY", "LEAST_LOADED")));
        assertEquals(AssignmentPolicy.LEAST_LOADED, new SystemSettingService(settingRepository).getAssignmentPolicy());
    }

    @Test
    void unknownStrategyIsRefused() {
        SystemSettingService real = new SystemSettingService(settingRepository);
        assertThrows(IllegalArgumentException.class, () -> real.setAssignmentPolicy("ROUND_ROBIN_MAGIC"));
        assertEquals(AssignmentPolicy.SKILL_ONLY, real.setAssignmentPolicy("skill_only"));
    }
}
