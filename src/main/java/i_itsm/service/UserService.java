package i_itsm.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import i_itsm.model.User;
import i_itsm.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "User not found: " + username));
    }

    /** Lets a logged-in user change their own password. The current password must be correct. */
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = findByUsername(username);

        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters");
        }
        if (newPassword.equals(currentPassword)) {
            throw new IllegalArgumentException("New password must be different from the current one");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
