package i_itsm.service;

import org.springframework.stereotype.Service;

import i_itsm.model.User;
import i_itsm.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "User not found: " + username));
    }
}