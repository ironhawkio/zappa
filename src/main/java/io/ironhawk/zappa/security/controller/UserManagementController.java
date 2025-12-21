package io.ironhawk.zappa.security.controller;

import io.ironhawk.zappa.security.dto.CreateUserRequest;
import io.ironhawk.zappa.security.entity.User;
import io.ironhawk.zappa.security.service.DatabaseUserDetailsService;
import io.ironhawk.zappa.security.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementController {

    @Autowired
    private UserManagementService userManagementService;

    @GetMapping
    public String listUsers(@RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String search,
                          @RequestParam(required = false) String role,
                          Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> users;

        if (search != null && !search.trim().isEmpty()) {
            users = userManagementService.searchUsers(search.trim(), pageable);
            model.addAttribute("search", search);
        } else if (role != null && !role.trim().isEmpty()) {
            try {
                User.Role roleEnum = User.Role.valueOf(role.toUpperCase());
                users = userManagementService.getUsersByRole(roleEnum, pageable);
                model.addAttribute("roleFilter", role);
            } catch (IllegalArgumentException e) {
                users = userManagementService.getAllUsers(pageable);
            }
        } else {
            users = userManagementService.getAllUsers(pageable);
        }

        model.addAttribute("users", users);
        model.addAttribute("totalUsers", userManagementService.getTotalUsers());
        model.addAttribute("adminCount", userManagementService.getUsersByRoleCount(User.Role.ADMIN));
        model.addAttribute("userCount", userManagementService.getUsersByRoleCount(User.Role.USER));
        model.addAttribute("roles", User.Role.values());

        return "admin/users/list";
    }

    @GetMapping("/new")
    public String showCreateUserForm(Model model) {
        model.addAttribute("createUserRequest", new CreateUserRequest());
        model.addAttribute("roles", User.Role.values());
        return "admin/users/create";
    }

    @PostMapping("/new")
    public String createUser(@Valid @ModelAttribute CreateUserRequest request,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {

        if (!request.isPasswordMatch()) {
            bindingResult.rejectValue("confirmPassword", "error.confirmPassword", "Passwords do not match");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", User.Role.values());
            return "admin/users/create";
        }

        try {
            User user = userManagementService.createUser(
                request.getUsername(),
                request.getEmail(),
                request.getFullName(),
                request.getPassword(),
                request.getRole()
            );

            redirectAttributes.addFlashAttribute("successMessage",
                "User '" + user.getUsername() + "' created successfully");
            return "redirect:/admin/users";

        } catch (Exception e) {
            bindingResult.rejectValue("username", "error.username", e.getMessage());
            model.addAttribute("roles", User.Role.values());
            return "admin/users/create";
        }
    }

    @GetMapping("/{userId}")
    public String viewUser(@PathVariable UUID userId, Model model) {
        User user = userManagementService.getUserById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        model.addAttribute("user", user);
        return "admin/users/view";
    }

    @PostMapping("/{userId}/toggle-enabled")
    public String toggleUserEnabled(@PathVariable UUID userId,
                                  @AuthenticationPrincipal DatabaseUserDetailsService.CustomUserPrincipal currentUser,
                                  RedirectAttributes redirectAttributes) {
        try {
            if (currentUser.getUser().getId().equals(userId)) {
                redirectAttributes.addFlashAttribute("errorMessage", "You cannot disable your own account");
                return "redirect:/admin/users";
            }

            userManagementService.toggleUserEnabled(userId);
            redirectAttributes.addFlashAttribute("successMessage", "User status updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/{userId}/delete")
    public String deleteUser(@PathVariable UUID userId,
                           @AuthenticationPrincipal DatabaseUserDetailsService.CustomUserPrincipal currentUser,
                           RedirectAttributes redirectAttributes) {
        try {
            if (currentUser.getUser().getId().equals(userId)) {
                redirectAttributes.addFlashAttribute("errorMessage", "You cannot delete your own account");
                return "redirect:/admin/users";
            }

            userManagementService.deleteUser(userId);
            redirectAttributes.addFlashAttribute("successMessage", "User deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/users";
    }

    @GetMapping("/{userId}/change-password")
    public String showChangePasswordForm(@PathVariable UUID userId, Model model) {
        User user = userManagementService.getUserById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        model.addAttribute("user", user);
        return "admin/users/change-password";
    }

    @PostMapping("/{userId}/change-password")
    public String changePassword(@PathVariable UUID userId,
                               @RequestParam String newPassword,
                               @RequestParam String confirmPassword,
                               RedirectAttributes redirectAttributes) {

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Passwords do not match");
            return "redirect:/admin/users/" + userId + "/change-password";
        }

        if (newPassword.length() < 8) {
            redirectAttributes.addFlashAttribute("errorMessage", "Password must be at least 8 characters long");
            return "redirect:/admin/users/" + userId + "/change-password";
        }

        try {
            userManagementService.changePassword(userId, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Password changed successfully");
            return "redirect:/admin/users/" + userId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/users/" + userId + "/change-password";
        }
    }
}