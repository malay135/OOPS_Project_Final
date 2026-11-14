package i_itsm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import i_itsm.model.User;
import i_itsm.repository.UserRepository;
import i_itsm.service.UserService;

/** Changing your own password, and saving your email address. */
class UserServiceTest {

    @Mock private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private UserService userService;
    private User riya;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userService = new UserService(userRepository, passwordEncoder);
        riya = new User("riya", passwordEncoder.encode("riya123"), "EMPLOYEE");
        when(userRepository.findByUsername("riya")).thenReturn(Optional.of(riya));
    }

    @Test
    void correctCurrentPasswordChangesIt() {
        userService.changePassword("riya", "riya123", "newpass1");

        assertTrue(passwordEncoder.matches("newpass1", riya.getPassword()));
        verify(userRepository).save(riya);
    }

    @Test
    void wrongCurrentPasswordIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.changePassword("riya", "wrong", "newpass1"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void shortNewPasswordIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.changePassword("riya", "riya123", "abc"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void samePasswordIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.changePassword("riya", "riya123", "riya123"));
    }

    @Test
    void emailIsTrimmedAndSaved() {
        assertEquals("riya@example.com", userService.updateEmail("riya", "  riya@example.com "));
        assertEquals("riya@example.com", riya.getEmail());
        verify(userRepository).save(riya);
    }

    @Test
    void blankEmailRemovesIt() {
        riya.setEmail("riya@example.com");
        assertNull(userService.updateEmail("riya", " "));
        assertNull(riya.getEmail());
    }

    @Test
    void invalidEmailIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> userService.updateEmail("riya", "riya at example"));
        assertThrows(IllegalArgumentException.class, () -> userService.updateEmail("riya", "riya@example"));
        verify(userRepository, never()).save(any());
    }
}
