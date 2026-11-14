package i_itsm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import i_itsm.model.Technician;
import i_itsm.model.User;
import i_itsm.repository.TechnicianRepository;
import i_itsm.repository.UserRepository;
import i_itsm.service.TechnicianService;

/** Linking a technician to the login they sign in with. */
class TechnicianServiceTest {

    @Mock private TechnicianRepository technicianRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private TechnicianService technicianService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        technicianService = new TechnicianService(technicianRepository, userRepository, passwordEncoder);
        when(technicianRepository.save(any(Technician.class))).thenAnswer(call -> call.getArgument(0));
        when(passwordEncoder.encode("secret1")).thenReturn("ENCODED");
    }

    private Technician form(String name, String username, String password) {
        Technician technician = new Technician(null, name, Set.of("HARDWARE"), 0);
        technician.setUsername(username);
        technician.setPassword(password);
        return technician;
    }

    @Test
    void newLoginIsCreatedAsTechnicianAccount() {
        Technician saved = technicianService.createTechnician(form("Neha Iyer", "neha", "secret1"));

        assertEquals("neha", saved.getUsername());
        verify(userRepository).save(argThat(user ->
                user.getUsername().equals("neha")
                        && user.getRole().equals("TECHNICIAN")
                        && user.getPassword().equals("ENCODED")));
    }

    @Test
    void existingTechnicianLoginIsLinkedWithoutPassword() {
        when(userRepository.findByUsername("technician"))
                .thenReturn(Optional.of(new User("technician", "x", "TECHNICIAN")));

        Technician saved = technicianService.createTechnician(form("Ravi Kumar", "technician", null));

        assertEquals("technician", saved.getUsername());
        verify(userRepository, never()).save(any());
    }

    @Test
    void employeeLoginCannotBeLinked() {
        when(userRepository.findByUsername("employee"))
                .thenReturn(Optional.of(new User("employee", "x", "EMPLOYEE")));

        assertThrows(IllegalArgumentException.class,
                () -> technicianService.createTechnician(form("Someone", "employee", null)));
    }

    @Test
    void loginAlreadyLinkedToAnotherTechnicianIsRejected() {
        Technician ravi = new Technician(1L, "Ravi Kumar", Set.of("HARDWARE"), 0);
        when(technicianRepository.findByUsername("technician")).thenReturn(Optional.of(ravi));

        assertThrows(IllegalArgumentException.class,
                () -> technicianService.createTechnician(form("Priya Sharma", "technician", null)));
    }

    @Test
    void newLoginWithoutPasswordIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> technicianService.createTechnician(form("Neha Iyer", "neha", "")));
    }

    @Test
    void blankLoginLeavesTechnicianUnlinked() {
        Technician saved = technicianService.createTechnician(form("Arjun Mehta", "  ", null));

        assertNull(saved.getUsername());
    }
}
