package i_itsm.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import i_itsm.model.Technician;
import i_itsm.model.User;
import i_itsm.repository.TechnicianRepository;
import i_itsm.repository.UserRepository;
import i_itsm.service.UserService;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final TechnicianRepository technicianRepository;

    public AuthController(UserRepository userRepository, UserService userService, PasswordEncoder passwordEncoder,
                          TechnicianRepository technicianRepository) {
        this.technicianRepository = technicianRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/me")
    public Map<String, Object> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        Map<String, Object> response = new HashMap<>();
        response.put("username", user.getUsername());
        response.put("role", user.getRole());

        // A technician login is linked to its Technician record by username (set on the admin Technicians page)
        if ("TECHNICIAN".equals(user.getRole())) {
            response.put("technicianId",
                    technicianRepository.findByUsername(username).map(Technician::getId).orElse(null));
        }

        return response;
    }

    // A logged-in user changes their own password: body {"currentPassword": "...", "newPassword": "..."}
    @PutMapping("/me/password")
    public Map<String, Object> changePassword(@RequestBody Map<String, String> body) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.changePassword(username, body.get("currentPassword"), body.get("newPassword"));
        return Map.of("message", "Password changed");
    }

    @PostMapping("/auth/register")
    public Map<String, Object> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }

        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already taken");
        }

        User user = new User(username, passwordEncoder.encode(password), "EMPLOYEE");
        User saved = userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("username", saved.getUsername());
        response.put("role", saved.getRole());
        return response;
    }
}