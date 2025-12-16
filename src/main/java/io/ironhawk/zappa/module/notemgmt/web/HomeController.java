package io.ironhawk.zappa.module.notemgmt.web;

import lombok.RequiredArgsConstructor;
import io.ironhawk.zappa.module.notemgmt.entity.Group;
import io.ironhawk.zappa.module.notemgmt.service.GroupService;
import io.ironhawk.zappa.module.notemgmt.service.NoteService;
import io.ironhawk.zappa.module.notemgmt.service.TagService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final NoteService noteService;
    private final TagService tagService;
    private final GroupService groupService;

    @GetMapping("/")
    public String home(Model model) {
        // Get basic stats
        long totalNotes = noteService.getAllNotes().size();
        long totalTags = tagService.getAllTags().size();

        // Get groups with note counts
        List<Object[]> groupsWithCounts = groupService.getGroupsWithNoteCounts();
        List<Group> allGroups = groupService.getRootGroups();

        model.addAttribute("totalNotes", totalNotes);
        model.addAttribute("totalTags", totalTags);
        model.addAttribute("groupsWithCounts", groupsWithCounts);
        model.addAttribute("allGroups", allGroups);

        return "index";
    }
}