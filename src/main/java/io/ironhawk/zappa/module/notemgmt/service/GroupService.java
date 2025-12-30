package io.ironhawk.zappa.module.notemgmt.service;

import io.ironhawk.zappa.module.notemgmt.entity.Group;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupService {

    // Basic CRUD operations
    Group createGroup(Group group);
    Optional<Group> getGroupById(UUID id);
    Group updateGroup(Group group);
    void deleteGroup(UUID id);
    List<Group> getAllGroups();

    // Hierarchy operations
    List<Group> getRootGroups();
    List<Group> getSubGroups(UUID parentGroupId);
    List<Group> getAllGroupsWithParent();

    // Search operations
    Optional<Group> findByName(String name);
    List<Group> searchGroups(String searchTerm);
    Group getOrCreateGroup(String name, String description);

    // Group management
    Group createSubGroup(UUID parentGroupId, String name, String description);
    void moveGroup(UUID groupId, UUID newParentGroupId);
    boolean canDeleteGroup(UUID groupId); // Check if group has notes or subgroups

    // Analytics
    Long countNotesInGroup(UUID groupId);
    List<Object[]> getGroupsWithNoteCounts();

    // Utility methods
    List<Group> getGroupHierarchy(UUID groupId); // Get full path from root to group
    boolean hasAnyGroups(); // Check if current user has any groups
    Group createDefaultGroup(); // Create default group if user has none
    Group getDefaultGroup(); // Get or create default group
    Group findDefaultGroup(); // Find existing default group (may return null)
}