package io.ironhawk.zappa.security.service;

import io.ironhawk.zappa.security.entity.User;
import io.ironhawk.zappa.security.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CurrentUserService {

    @Autowired
    private UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }

        if (authentication.getPrincipal() instanceof DatabaseUserDetailsService.CustomUserPrincipal) {
            return ((DatabaseUserDetailsService.CustomUserPrincipal) authentication.getPrincipal()).getUser();
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    public UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public String getCurrentUsername() {
        return getCurrentUser().getUsername();
    }

    public boolean isCurrentUserAdmin() {
        return getCurrentUser().getRole() == User.Role.ADMIN;
    }

    public boolean isCurrentUser(UUID userId) {
        return getCurrentUserId().equals(userId);
    }

    public boolean isCurrentUser(String username) {
        return getCurrentUsername().equals(username);
    }
}