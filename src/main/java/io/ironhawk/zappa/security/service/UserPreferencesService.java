package io.ironhawk.zappa.security.service;

import io.ironhawk.zappa.security.entity.User;
import io.ironhawk.zappa.security.entity.UserPreferences;
import io.ironhawk.zappa.security.repository.UserPreferencesRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserPreferencesService {

    private final UserPreferencesRepository userPreferencesRepository;
    private final ObjectMapper objectMapper;

    public Map<String, Object> getGraphPositions(UUID userId) {
        Optional<String> positionsJson = userPreferencesRepository.findGraphPositionsByUserId(userId);

        if (positionsJson.isEmpty() || positionsJson.get() == null) {
            return new HashMap<>();
        }

        try {
            return objectMapper.readValue(positionsJson.get(), Map.class);
        } catch (JsonProcessingException e) {
            log.error("Error parsing graph positions for user: {}", userId, e);
            return new HashMap<>();
        }
    }

    public void saveGraphPositions(UUID userId, Map<String, Object> positions) {
        try {
            String positionsJson = objectMapper.writeValueAsString(positions);

            if (userPreferencesRepository.existsByUserId(userId)) {
                userPreferencesRepository.updateGraphPositions(userId, positionsJson);
            } else {
                // Create new preferences record
                User user = new User();
                user.setId(userId);

                UserPreferences preferences = UserPreferences.builder()
                    .user(user)
                    .graphPositions(positionsJson)
                    .build();

                userPreferencesRepository.save(preferences);
            }

            log.debug("Saved graph positions for user: {}", userId);
        } catch (JsonProcessingException e) {
            log.error("Error serializing graph positions for user: {}", userId, e);
            throw new RuntimeException("Failed to save graph positions", e);
        }
    }

    public Map<String, Object> getGraphPositionsForGroup(UUID userId, String groupKey) {
        Map<String, Object> allPositions = getGraphPositions(userId);
        Object groupPositions = allPositions.get(groupKey);

        if (groupPositions instanceof Map) {
            return (Map<String, Object>) groupPositions;
        }

        return new HashMap<>();
    }

    public void saveGraphPositionsForGroup(UUID userId, String groupKey, Map<String, Object> positions) {
        Map<String, Object> allPositions = getGraphPositions(userId);
        allPositions.put(groupKey, positions);
        saveGraphPositions(userId, allPositions);
    }

    public void clearGraphPositions(UUID userId) {
        Optional<UserPreferences> preferences = userPreferencesRepository.findByUserId(userId);
        if (preferences.isPresent()) {
            preferences.get().setGraphPositions(null);
            userPreferencesRepository.save(preferences.get());
        }
    }

    public void clearGraphPositionsForGroup(UUID userId, String groupKey) {
        Map<String, Object> allPositions = getGraphPositions(userId);
        allPositions.remove(groupKey);

        if (allPositions.isEmpty()) {
            clearGraphPositions(userId);
        } else {
            saveGraphPositions(userId, allPositions);
        }
    }

    public boolean hasGraphPositions(UUID userId) {
        return userPreferencesRepository.findGraphPositionsByUserId(userId)
            .map(positions -> positions != null && !positions.trim().isEmpty())
            .orElse(false);
    }
}