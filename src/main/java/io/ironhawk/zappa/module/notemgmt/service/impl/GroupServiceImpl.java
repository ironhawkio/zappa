package io.ironhawk.zappa.module.notemgmt.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.ironhawk.zappa.module.notemgmt.entity.Group;
import io.ironhawk.zappa.module.notemgmt.repository.GroupRepository;
import io.ironhawk.zappa.module.notemgmt.service.GroupService;
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

    @Override
    @Transactional
    public Group createGroup(Group group) {
        log.info("Creating new group with name: {}", group.getName());

        if (groupRepository.existsByName(group.getName())) {
            throw new IllegalArgumentException("Group with name '" + group.getName() + "' already exists");
        }

        return groupRepository.save(group);
    }

    @Override
    public Optional<Group> getGroupById(UUID id) {
        log.debug("Fetching group with id: {}", id);
        return groupRepository.findById(id);
    }

    @Override
    @Transactional
    public Group updateGroup(Group group) {
        log.info("Updating group with id: {}", group.getId());

        if (!groupRepository.existsById(group.getId())) {
            throw new IllegalArgumentException("Group not found with id: " + group.getId());
        }

        // Check for name conflicts (excluding current group)
        Optional<Group> existingGroup = groupRepository.findByName(group.getName());
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
        log.debug("Fetching all groups");
        return groupRepository.findAllWithParent();
    }

    @Override
    public List<Group> getRootGroups() {
        log.debug("Fetching root groups");
        return groupRepository.findByParentGroupIsNullOrderBySortOrderAscNameAsc();
    }

    @Override
    public List<Group> getSubGroups(UUID parentGroupId) {
        log.debug("Fetching subgroups for parent group: {}", parentGroupId);
        return groupRepository.findByParentGroupIdOrderBySortOrderAscNameAsc(parentGroupId);
    }

    @Override
    public List<Group> getAllGroupsWithParent() {
        return groupRepository.findAllWithParent();
    }

    @Override
    public Optional<Group> findByName(String name) {
        log.debug("Finding group by name: {}", name);
        return groupRepository.findByName(name);
    }

    @Override
    public List<Group> searchGroups(String searchTerm) {
        log.debug("Searching groups with term: {}", searchTerm);
        return groupRepository.searchGroups(searchTerm);
    }

    @Override
    @Transactional
    public Group getOrCreateGroup(String name, String description) {
        log.debug("Getting or creating group: {}", name);

        return groupRepository.findByName(name)
            .orElseGet(() -> {
                Group newGroup = Group.of(name, description);
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
        log.debug("Counting notes in group: {}", groupId);
        return groupRepository.countNotesInGroup(groupId);
    }

    @Override
    public List<Object[]> getGroupsWithNoteCounts() {
        log.debug("Fetching groups with note counts");
        return groupRepository.findGroupsWithNoteCounts();
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
    public Group getDefaultGroup() {
        log.debug("Getting default group");
        return groupRepository.findByName("General")
            .orElseGet(() -> {
                log.warn("Default 'General' group not found, creating it");
                Group defaultGroup = Group.of("General", "General notes and miscellaneous content", "#6c757d", "fas fa-sticky-note");
                return groupRepository.save(defaultGroup);
            });
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