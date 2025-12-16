package io.ironhawk.zappa.module.notemgmt.repository;

import io.ironhawk.zappa.module.notemgmt.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {

    // Find by name
    Optional<Group> findByName(String name);
    boolean existsByName(String name);

    // Find root groups (no parent)
    List<Group> findByParentGroupIsNullOrderBySortOrderAscNameAsc();

    // Find subgroups of a parent
    List<Group> findByParentGroupOrderBySortOrderAscNameAsc(Group parentGroup);
    List<Group> findByParentGroupIdOrderBySortOrderAscNameAsc(UUID parentGroupId);

    // Find all groups with their hierarchy
    @Query("SELECT g FROM Group g LEFT JOIN FETCH g.parentGroup ORDER BY g.sortOrder ASC, g.name ASC")
    List<Group> findAllWithParent();

    // Find groups by color
    List<Group> findByColor(String color);

    // Count notes in group (direct only, not including subgroups)
    @Query("SELECT COUNT(n) FROM Note n WHERE n.group.id = :groupId")
    Long countNotesInGroup(@Param("groupId") UUID groupId);

    // Find groups with note counts
    @Query("SELECT g, COUNT(n) FROM Group g LEFT JOIN g.notes n GROUP BY g ORDER BY g.sortOrder ASC, g.name ASC")
    List<Object[]> findGroupsWithNoteCounts();

    // Search groups by name or description
    @Query("SELECT g FROM Group g WHERE " +
           "LOWER(g.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(g.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Group> searchGroups(@Param("searchTerm") String searchTerm);
}