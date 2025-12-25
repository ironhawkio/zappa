package io.ironhawk.zappa.module.notemgmt.controller;

import io.ironhawk.zappa.module.notemgmt.dto.CreateGroupRequest;
import io.ironhawk.zappa.module.notemgmt.entity.Group;
import io.ironhawk.zappa.module.notemgmt.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/groups")
public class GroupManagementController {

    @Autowired
    private GroupService groupService;

    @GetMapping
    public String listGroups(Model model) {
        List<Group> groups = groupService.getAllGroups();
        List<Object[]> groupsWithCounts = groupService.getGroupsWithNoteCounts();

        model.addAttribute("groups", groups);
        model.addAttribute("groupsWithCounts", groupsWithCounts);
        model.addAttribute("totalGroups", groups.size());

        return "groups/list";
    }

    @GetMapping("/new")
    public String showCreateGroupForm(Model model) {
        model.addAttribute("createGroupRequest", new CreateGroupRequest());
        model.addAttribute("parentGroups", groupService.getAllGroups());
        return "groups/create";
    }

    @PostMapping("/new")
    public String createGroup(@Valid @ModelAttribute CreateGroupRequest request,
                            BindingResult bindingResult,
                            Model model,
                            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("parentGroups", groupService.getAllGroups());
            return "groups/create";
        }

        try {
            Group parentGroup = null;
            if (request.getParentGroupId() != null) {
                parentGroup = groupService.getGroupById(request.getParentGroupId())
                    .orElseThrow(() -> new RuntimeException("Parent group not found"));
            }

            Group group = Group.builder()
                .name(request.getName())
                .description(request.getDescription())
                .color(request.getColor())
                .icon(request.getIcon())
                .parentGroup(parentGroup)
                .sortOrder(request.getSortOrder())
                .build();

            Group createdGroup = groupService.createGroup(group);
            redirectAttributes.addFlashAttribute("successMessage",
                "Group '" + createdGroup.getName() + "' created successfully");
            return "redirect:/groups";

        } catch (Exception e) {
            bindingResult.rejectValue("name", "error.name", e.getMessage());
            model.addAttribute("parentGroups", groupService.getAllGroups());
            return "groups/create";
        }
    }

    @GetMapping("/{groupId}")
    public String viewGroup(@PathVariable UUID groupId, Model model) {
        Group group = groupService.getGroupById(groupId)
            .orElseThrow(() -> new RuntimeException("Group not found"));

        List<Group> subGroups = groupService.getSubGroups(groupId);
        Long noteCount = groupService.countNotesInGroup(groupId);

        model.addAttribute("group", group);
        model.addAttribute("subGroups", subGroups);
        model.addAttribute("noteCount", noteCount);
        model.addAttribute("hierarchy", groupService.getGroupHierarchy(groupId));

        return "groups/view";
    }

    @GetMapping("/{groupId}/edit")
    public String showEditGroupForm(@PathVariable UUID groupId, Model model) {
        Group group = groupService.getGroupById(groupId)
            .orElseThrow(() -> new RuntimeException("Group not found"));

        CreateGroupRequest request = new CreateGroupRequest();
        request.setName(group.getName());
        request.setDescription(group.getDescription());
        request.setColor(group.getColor());
        request.setIcon(group.getIcon());
        request.setSortOrder(group.getSortOrder());
        if (group.getParentGroup() != null) {
            request.setParentGroupId(group.getParentGroup().getId());
        }

        model.addAttribute("createGroupRequest", request);
        model.addAttribute("group", group);
        model.addAttribute("parentGroups", groupService.getAllGroups());

        return "groups/edit";
    }

    @PostMapping("/{groupId}/edit")
    public String updateGroup(@PathVariable UUID groupId,
                            @Valid @ModelAttribute CreateGroupRequest request,
                            BindingResult bindingResult,
                            Model model,
                            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            Group group = groupService.getGroupById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
            model.addAttribute("group", group);
            model.addAttribute("parentGroups", groupService.getAllGroups());
            return "groups/edit";
        }

        try {
            Group existingGroup = groupService.getGroupById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

            Group parentGroup = null;
            if (request.getParentGroupId() != null) {
                parentGroup = groupService.getGroupById(request.getParentGroupId())
                    .orElseThrow(() -> new RuntimeException("Parent group not found"));
            }

            existingGroup.setName(request.getName());
            existingGroup.setDescription(request.getDescription());
            existingGroup.setColor(request.getColor());
            existingGroup.setIcon(request.getIcon());
            existingGroup.setParentGroup(parentGroup);
            existingGroup.setSortOrder(request.getSortOrder());

            groupService.updateGroup(existingGroup);
            redirectAttributes.addFlashAttribute("successMessage",
                "Group '" + existingGroup.getName() + "' updated successfully");
            return "redirect:/groups/" + groupId;

        } catch (Exception e) {
            bindingResult.rejectValue("name", "error.name", e.getMessage());
            Group group = groupService.getGroupById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
            model.addAttribute("group", group);
            model.addAttribute("parentGroups", groupService.getAllGroups());
            return "groups/edit";
        }
    }

    @PostMapping("/{groupId}/delete")
    public String deleteGroup(@PathVariable UUID groupId,
                            RedirectAttributes redirectAttributes) {
        try {
            Group group = groupService.getGroupById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

            if (!groupService.canDeleteGroup(groupId)) {
                redirectAttributes.addFlashAttribute("errorMessage",
                    "Cannot delete group '" + group.getName() + "': it contains notes or subgroups");
                return "redirect:/groups";
            }

            String groupName = group.getName();
            groupService.deleteGroup(groupId);
            redirectAttributes.addFlashAttribute("successMessage",
                "Group '" + groupName + "' deleted successfully");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Error deleting group: " + e.getMessage());
        }

        return "redirect:/groups";
    }
}