package io.ironhawk.zappa.security.service;

import io.ironhawk.zappa.security.entity.User;
import io.ironhawk.zappa.security.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UserManagementService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public Page<User> searchUsers(String searchTerm, Pageable pageable) {
        if (!StringUtils.hasText(searchTerm)) {
            return getAllUsers(pageable);
        }
        return userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(
            searchTerm, searchTerm, searchTerm, pageable);
    }

    public Page<User> getUsersByRole(User.Role role, Pageable pageable) {
        return userRepository.findByRoleOrderByCreatedAtDesc(role, pageable);
    }

    public Optional<User> getUserById(UUID id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User createUser(String username, String email, String fullName, String password, User.Role role) {
        validateUserCreation(username, email);

        User user = User.builder()
            .username(username)
            .email(email)
            .fullName(fullName)
            .password(passwordEncoder.encode(password))
            .role(role)
            .enabled(true)
            .build();

        return userRepository.save(user);
    }

    public User updateUser(UUID userId, String email, String fullName, User.Role role) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists: " + email);
        }

        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole(role);

        return userRepository.save(user);
    }

    public void changePassword(UUID userId, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void toggleUserEnabled(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        user.setEnabled(!user.getEnabled());
        userRepository.save(user);
    }

    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() == User.Role.ADMIN) {
            long adminCount = userRepository.countByRole(User.Role.ADMIN);
            if (adminCount <= 1) {
                throw new RuntimeException("Cannot delete the last admin user");
            }
        }

        userRepository.delete(user);
    }

    public long getTotalUsers() {
        return userRepository.count();
    }

    public long getUsersByRoleCount(User.Role role) {
        return userRepository.countByRole(role);
    }

    private void validateUserCreation(String username, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists: " + email);
        }
    }

}