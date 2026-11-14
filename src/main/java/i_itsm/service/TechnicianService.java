package i_itsm.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import i_itsm.model.Technician;
import i_itsm.model.User;
import i_itsm.repository.TechnicianRepository;
import i_itsm.repository.UserRepository;

@Service
public class TechnicianService {

    private final TechnicianRepository technicianRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TechnicianService(
            TechnicianRepository technicianRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.technicianRepository = technicianRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Technician createTechnician(Technician technician) {
        technician.setId(null);
        applyLogin(technician, null, technician.getUsername(), technician.getPassword());
        return technicianRepository.save(technician);
    }

    public List<Technician> getAllTechnicians() {
        return technicianRepository.findAll();
    }

    public Technician getTechnicianById(Long id) {
        return technicianRepository.findById(id)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Technician not found: " + id));
    }

    @Transactional
    public Technician updateTechnician(
            Long id, Technician updatedTechnician) {

        Technician existing = getTechnicianById(id);

        existing.setName(updatedTechnician.getName());
        existing.setSkills(updatedTechnician.getSkills());
        existing.setActiveIncidentCount(
                updatedTechnician.getActiveIncidentCount());
        existing.setActive(updatedTechnician.isActive());
        applyLogin(existing, id, updatedTechnician.getUsername(), updatedTechnician.getPassword());

        return technicianRepository.save(existing);
    }

    /**
     * Links a technician to a login (users table).
     * - blank username: not linked
     * - existing TECHNICIAN login: linked (a password given here resets it)
     * - new username: a TECHNICIAN login is created with the given password
     */
    private void applyLogin(Technician target, Long ownId, String username, String password) {
        String login = username == null ? "" : username.trim();
        if (login.isEmpty()) {
            target.setUsername(null);
            return;
        }

        Optional<Technician> linked = technicianRepository.findByUsername(login);
        if (linked.isPresent() && !linked.get().getId().equals(ownId)) {
            throw new IllegalArgumentException(
                    "Login '" + login + "' is already linked to " + linked.get().getName());
        }

        boolean passwordGiven = password != null && !password.isBlank();
        if (passwordGiven && password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }

        Optional<User> account = userRepository.findByUsername(login);
        if (account.isPresent()) {
            User user = account.get();
            if (!"TECHNICIAN".equals(user.getRole())) {
                throw new IllegalArgumentException(
                        "Login '" + login + "' is not a technician account");
            }
            if (passwordGiven) {
                user.setPassword(passwordEncoder.encode(password));
                userRepository.save(user);
            }
        } else {
            if (!passwordGiven) {
                throw new IllegalArgumentException(
                        "Login '" + login + "' does not exist yet. Enter a password to create it");
            }
            userRepository.save(new User(login, passwordEncoder.encode(password), "TECHNICIAN"));
        }

        target.setUsername(login);
    }
}
