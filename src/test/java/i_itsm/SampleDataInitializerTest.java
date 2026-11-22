package i_itsm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import i_itsm.config.SampleDataInitializer;
import i_itsm.model.Incident;
import i_itsm.model.Technician;
import i_itsm.model.User;
import i_itsm.repository.ITServiceRepository;
import i_itsm.repository.IncidentRepository;
import i_itsm.repository.TechnicianRepository;
import i_itsm.repository.UserRepository;
import i_itsm.service.FeedbackService;
import i_itsm.service.IncidentService;

/** Sample data only fills an empty database, and the default technician login gets linked to a technician. */
class SampleDataInitializerTest {

    @Mock private TechnicianRepository technicianRepository;
    @Mock private UserRepository userRepository;
    @Mock private IncidentRepository incidentRepository;
    @Mock private ITServiceRepository serviceRepository;
    @Mock private IncidentService incidentService;
    @Mock private FeedbackService feedbackService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(userRepository.findByUsername("technician")).thenReturn(Optional.of(new User("technician", "x", "TECHNICIAN")));
        when(userRepository.findByUsername("priya")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("arjun")).thenReturn(Optional.empty());
    }

    private SampleDataInitializer loader(boolean enabled) {
        return new SampleDataInitializer(technicianRepository, userRepository, incidentRepository, serviceRepository,
                incidentService, feedbackService, new BCryptPasswordEncoder(), enabled);
    }

    @Test
    void emptyDatabaseGetsThreeTechniciansWithLogins() {
        when(technicianRepository.count()).thenReturn(0L, 3L);
        when(technicianRepository.findByUsername("technician")).thenReturn(Optional.of(new Technician()));
        when(incidentRepository.count()).thenReturn(5L); // incidents already there: only technicians are added

        loader(true).loadSampleData();

        ArgumentCaptor<Technician> saved = ArgumentCaptor.forClass(Technician.class);
        verify(technicianRepository, times(3)).save(saved.capture());
        assertEquals(List.of("technician", "priya", "arjun"), saved.getAllValues().stream().map(Technician::getUsername).toList());
        assertEquals(Set.of("HARDWARE"), saved.getAllValues().get(0).getSkills());
        verify(userRepository, times(2)).save(any(User.class)); // priya and arjun; "technician" already exists
        verifyNoInteractions(incidentService);
    }

    @Test
    void unlinkedDefaultLoginGoesToTheFirstTechnicianWithoutALogin() {
        Technician hasLogin = new Technician(1L, "Neha Iyer", Set.of("CLOUD"), 0);
        hasLogin.setUsername("neha");
        Technician second = new Technician(2L, "Ravi Kumar", Set.of("HARDWARE"), 0);
        Technician later = new Technician(5L, "Priya Sharma", Set.of("NETWORK"), 0);
        when(technicianRepository.count()).thenReturn(3L);
        when(technicianRepository.findByUsername("technician")).thenReturn(Optional.empty());
        when(technicianRepository.findAll()).thenReturn(new ArrayList<>(List.of(later, hasLogin, second)));
        when(incidentRepository.count()).thenReturn(2L);

        loader(true).loadSampleData();

        assertEquals("technician", second.getUsername());
        assertNull(later.getUsername());
        assertEquals("neha", hasLogin.getUsername());
        verify(technicianRepository).save(second);
    }

    @Test
    void databaseWithDataIsLeftAlone() {
        when(technicianRepository.count()).thenReturn(4L);
        when(technicianRepository.findByUsername("technician")).thenReturn(Optional.of(new Technician()));
        when(incidentRepository.count()).thenReturn(12L);

        loader(true).loadSampleData();

        verify(technicianRepository, never()).save(any());
        verify(userRepository, never()).save(any());
        verify(incidentService, never()).createIncident(any(Incident.class));
    }

    @Test
    void canBeSwitchedOff() {
        loader(false).loadSampleData();
        verifyNoInteractions(technicianRepository, incidentRepository, incidentService);
        verify(userRepository, never()).findByUsername(anyString());
    }
}
