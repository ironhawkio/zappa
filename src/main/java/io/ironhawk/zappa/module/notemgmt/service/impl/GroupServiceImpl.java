package io.ironhawk.zappa.module.notemgmt.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.ironhawk.zappa.module.notemgmt.entity.Group;
import io.ironhawk.zappa.module.notemgmt.repository.GroupRepository;
import io.ironhawk.zappa.module.notemgmt.service.GroupService;
import io.ironhawk.zappa.security.entity.User;
import io.ironhawk.zappa.security.service.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupServiceImpl implements GroupService {

    private final GroupRepository groupRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public Group createGroup(Group group) {
        User currentUser = currentUserService.getCurrentUser();
        group.setUser(currentUser);
        log.info("Creating new group with name: {} for user: {}", group.getName(), currentUser.getUsername());

        if (groupRepository.existsByUserAndName(currentUser, group.getName())) {
            throw new IllegalArgumentException("Group with name '" + group.getName() + "' already exists");
        }

        return groupRepository.save(group);
    }

    @Override
    public Optional<Group> getGroupById(UUID id) {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Fetching group with id: {} for user: {}", id, currentUser.getUsername());
        return groupRepository.findByIdAndUser(id, currentUser);
    }

    @Override
    @Transactional
    public Group updateGroup(Group group) {
        User currentUser = currentUserService.getCurrentUser();
        log.info("Updating group with id: {} for user: {}", group.getId(), currentUser.getUsername());

        if (!groupRepository.existsById(group.getId())) {
            throw new IllegalArgumentException("Group not found with id: " + group.getId());
        }

        // Check for name conflicts (excluding current group)
        Optional<Group> existingGroup = groupRepository.findByUserAndName(currentUser, group.getName());
        if (existingGroup.isPresent() && !existingGroup.get().getId().equals(group.getId())) {
            throw new IllegalArgumentException("Group with name '" + group.getName() + "' already exists");
        }

        return groupRepository.save(group);
    }

    @Override
    @Transactional
    public void deleteGroup(UUID id) {
        log.info("Deleting group with id: {}", id);

        if (!canDeleteGroup(id)) {
            throw new IllegalStateException("Cannot delete group: it contains notes or subgroups");
        }

        groupRepository.deleteById(id);
    }

    @Override
    public List<Group> getAllGroups() {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Fetching all groups for user: {}", currentUser.getUsername());
        return groupRepository.findAllWithParentByUser(currentUser);
    }

    @Override
    public List<Group> getRootGroups() {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Fetching root groups for user: {}", currentUser.getUsername());
        return groupRepository.findByUserAndParentGroupIsNullOrderBySortOrderAscNameAsc(currentUser);
    }

    @Override
    public List<Group> getSubGroups(UUID parentGroupId) {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Fetching subgroups for parent group: {} for user: {}", parentGroupId, currentUser.getUsername());
        return groupRepository.findByUserAndParentGroupIdOrderBySortOrderAscNameAsc(currentUser, parentGroupId);
    }

    @Override
    public List<Group> getAllGroupsWithParent() {
        User currentUser = currentUserService.getCurrentUser();
        return groupRepository.findAllWithParentByUser(currentUser);
    }

    @Override
    public Optional<Group> findByName(String name) {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Finding group by name: {} for user: {}", name, currentUser.getUsername());
        return groupRepository.findByUserAndName(currentUser, name);
    }

    @Override
    public List<Group> searchGroups(String searchTerm) {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Searching groups with term: {} for user: {}", searchTerm, currentUser.getUsername());
        return groupRepository.searchGroupsByUser(currentUser, searchTerm);
    }

    @Override
    @Transactional
    public Group getOrCreateGroup(String name, String description) {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Getting or creating group: {} for user: {}", name, currentUser.getUsername());

        return groupRepository.findByUserAndName(currentUser, name)
            .orElseGet(() -> {
                Group newGroup = Group.of(name, description);
                newGroup.setUser(currentUser);
                return groupRepository.save(newGroup);
            });
    }

    @Override
    @Transactional
    public Group createSubGroup(UUID parentGroupId, String name, String description) {
        log.info("Creating subgroup '{}' under parent group: {}", name, parentGroupId);

        Group parentGroup = groupRepository.findById(parentGroupId)
            .orElseThrow(() -> new IllegalArgumentException("Parent group not found with id: " + parentGroupId));

        Group subGroup = Group.withParent(name, description, parentGroup);
        return groupRepository.save(subGroup);
    }

    @Override
    @Transactional
    public void moveGroup(UUID groupId, UUID newParentGroupId) {
        log.info("Moving group {} to new parent: {}", groupId, newParentGroupId);

        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Group not found with id: " + groupId));

        Group newParent = null;
        if (newParentGroupId != null) {
            newParent = groupRepository.findById(newParentGroupId)
                .orElseThrow(() -> new IllegalArgumentException("New parent group not found with id: " + newParentGroupId));

            // Prevent circular references
            if (isDescendantOf(newParent, group)) {
                throw new IllegalArgumentException("Cannot move group: would create circular reference");
            }
        }

        group.setParentGroup(newParent);
        groupRepository.save(group);
    }

    @Override
    public boolean canDeleteGroup(UUID groupId) {
        Group group = groupRepository.findById(groupId).orElse(null);
        if (group == null) {
            return false;
        }

        // Cannot delete if it has notes or subgroups
        return group.getNotes().isEmpty() && group.getSubGroups().isEmpty();
    }

    @Override
    public Long countNotesInGroup(UUID groupId) {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Counting notes in group: {} for user: {}", groupId, currentUser.getUsername());
        return groupRepository.countNotesInGroupByUser(currentUser, groupId);
    }

    @Override
    public List<Object[]> getGroupsWithNoteCounts() {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Fetching groups with note counts for user: {}", currentUser.getUsername());
        return groupRepository.findGroupsWithNoteCountsByUser(currentUser);
    }

    @Override
    public List<Group> getGroupHierarchy(UUID groupId) {
        log.debug("Getting hierarchy for group: {}", groupId);

        Group group = groupRepository.findById(groupId).orElse(null);
        if (group == null) {
            return List.of();
        }

        List<Group> hierarchy = new ArrayList<>();
        Group current = group;

        while (current != null) {
            hierarchy.add(0, current); // Add to beginning to maintain order
            current = current.getParentGroup();
        }

        return hierarchy;
    }

    @Override
    public boolean hasAnyGroups() {
        User currentUser = currentUserService.getCurrentUser();
        List<Group> userGroups = groupRepository.findByUserOrderBySortOrderAscNameAsc(currentUser);
        return !userGroups.isEmpty();
    }

    @Override
    @Transactional
    public Group createDefaultGroup() {
        User currentUser = currentUserService.getCurrentUser();
        log.debug("Ensuring default group exists for user: {}", currentUser.getUsername());

        if (hasAnyGroups()) {
            return groupRepository.findByUserAndParentGroupIsNullOrderBySortOrderAscNameAsc(currentUser)
                .stream()
                .findFirst()
                .orElse(null);
        }

        log.info("Creating default group for new user: {}", currentUser.getUsername());
        Group defaultGroup = Group.builder()
            .name("Default")
            .description("Default group for your notes")
            .color("#6c757d")
            .icon("fas fa-sticky-note")
            .user(currentUser)
            .sortOrder(0)
            .build();

        return groupRepository.save(defaultGroup);
    }

    @Override
    public Group getDefaultGroup() {
        Group defaultGroup = findDefaultGroup();
        if (defaultGroup != null) {
            return defaultGroup;
        }
        return createDefaultGroup();
    }

    @Override
    public Group findDefaultGroup() {
        User currentUser = currentUserService.getCurrentUser();

        // Look for a group named "Default" with no parent
        return groupRepository.findByUserAndNameIgnoreCaseAndParentGroupIsNull(currentUser, "Default")
            .orElse(null);
    }

    // Helper method to check for circular references
    private boolean isDescendantOf(Group potentialAncestor, Group potentialDescendant) {
        Group current = potentialAncestor.getParentGroup();
        while (current != null) {
            if (current.getId().equals(potentialDescendant.getId())) {
                return true;
            }
            current = current.getParentGroup();
        }
        return false;
    }
}