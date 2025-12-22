package io.ironhawk.zappa.module.notemgmt.repository;

import io.ironhawk.zappa.module.notemgmt.entity.Group;
import io.ironhawk.zappa.security.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {

    // User-specific group queries
    List<Group> findByUserOrderBySortOrderAscNameAsc(User user);
    Optional<Group> findByIdAndUser(UUID id, User user);

    // Find by name for specific user
    Optional<Group> findByUserAndName(User user, String name);
    boolean existsByUserAndName(User user, String name);

    // Find root groups (no parent) for specific user
    List<Group> findByUserAndParentGroupIsNullOrderBySortOrderAscNameAsc(User user);

    // Find subgroups of a parent for specific user
    List<Group> findByUserAndParentGroupOrderBySortOrderAscNameAsc(User user, Group parentGroup);
    List<Group> findByUserAndParentGroupIdOrderBySortOrderAscNameAsc(User user, UUID parentGroupId);

    // Find all groups with their hierarchy for specific user
    @Query("SELECT g FROM Group g LEFT JOIN FETCH g.parentGroup WHERE g.user = :user ORDER BY g.sortOrder ASC, g.name ASC")
    List<Group> findAllWithParentByUser(@Param("user") User user);

    // Find groups by color for specific user
    List<Group> findByUserAndColor(User user, String color);

    // Count notes in group (direct only, not including subgroups) for specific user
    @Query("SELECT COUNT(n) FROM Note n WHERE n.user = :user AND n.group.id = :groupId")
    Long countNotesInGroupByUser(@Param("user") User user, @Param("groupId") UUID groupId);

    // Find groups with note counts for specific user
    @Query("SELECT g, COUNT(n) FROM Group g LEFT JOIN g.notes n WHERE g.user = :user GROUP BY g ORDER BY g.sortOrder ASC, g.name ASC")
    List<Object[]> findGroupsWithNoteCountsByUser(@Param("user") User user);

    // Search groups by name or description for specific user
    @Query("SELECT g FROM Group g WHERE g.user = :user AND (" +
           "LOWER(g.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(g.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Group> searchGroupsByUser(@Param("user") User user, @Param("searchTerm") String searchTerm);
}