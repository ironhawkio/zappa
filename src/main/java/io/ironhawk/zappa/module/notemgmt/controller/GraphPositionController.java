package io.ironhawk.zappa.module.notemgmt.controller;

import io.ironhawk.zappa.security.service.CurrentUserService;
import io.ironhawk.zappa.security.service.UserPreferencesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/graph/positions")
@RequiredArgsConstructor
public class GraphPositionController {

    private final UserPreferencesService userPreferencesService;
    private final CurrentUserService currentUserService;

    @GetMapping("/{groupKey}")
    public ResponseEntity<Map<String, Object>> getPositions(@PathVariable String groupKey) {
        try {
            var currentUser = currentUserService.getCurrentUser();
            Map<String, Object> positions = userPreferencesService.getGraphPositionsForGroup(
                currentUser.getId(), groupKey);

            log.debug("Retrieved positions for user {} group {}: {} nodes",
                currentUser.getUsername(), groupKey, positions.size());

            return ResponseEntity.ok(positions);
        } catch (Exception e) {
            log.error("Error getting graph positions for group: {}", groupKey, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{groupKey}")
    public ResponseEntity<String> savePositions(
            @PathVariable String groupKey,
            @RequestBody Map<String, Object> positions) {
        try {
            var currentUser = currentUserService.getCurrentUser();
            userPreferencesService.saveGraphPositionsForGroup(
                currentUser.getId(), groupKey, positions);

            log.info("Saved positions for user {} group {}: {} nodes",
                currentUser.getUsername(), groupKey, positions.size());

            return ResponseEntity.ok("Positions saved successfully");
        } catch (Exception e) {
            log.error("Error saving graph positions for group: {}", groupKey, e);
            return ResponseEntity.internalServerError().body("Failed to save positions");
        }
    }

    @DeleteMapping("/{groupKey}")
    public ResponseEntity<String> clearPositions(@PathVariable String groupKey) {
        try {
            var currentUser = currentUserService.getCurrentUser();

            // Clear positions for specific group
            userPreferencesService.clearGraphPositionsForGroup(currentUser.getId(), groupKey);

            log.info("Cleared positions for user {} group {}", currentUser.getUsername(), groupKey);

            return ResponseEntity.ok("Positions cleared successfully");
        } catch (Exception e) {
            log.error("Error clearing graph positions for group: {}", groupKey, e);
            return ResponseEntity.internalServerError().body("Failed to clear positions");
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllPositions() {
        try {
            var currentUser = currentUserService.getCurrentUser();
            Map<String, Object> allPositions = userPreferencesService.getGraphPositions(currentUser.getId());

            log.debug("Retrieved all positions for user {}: {} groups",
                currentUser.getUsername(), allPositions.size());

            return ResponseEntity.ok(allPositions);
        } catch (Exception e) {
            log.error("Error getting all graph positions", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping
    public ResponseEntity<String> clearAllPositions() {
        try {
            var currentUser = currentUserService.getCurrentUser();
            userPreferencesService.clearGraphPositions(currentUser.getId());

            log.info("Cleared all positions for user {}", currentUser.getUsername());

            return ResponseEntity.ok("All positions cleared successfully");
        } catch (Exception e) {
            log.error("Error clearing all graph positions", e);
            return ResponseEntity.internalServerError().body("Failed to clear all positions");
        }
    }
}