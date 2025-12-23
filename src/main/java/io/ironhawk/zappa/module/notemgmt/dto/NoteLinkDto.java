package io.ironhawk.zappa.module.notemgmt.dto;

import io.ironhawk.zappa.module.notemgmt.entity.NoteLink;
import io.ironhawk.zappa.module.notemgmt.entity.NoteLinkType;
import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class NoteLinkDto {

    private UUID id;
    private UUID sourceNoteId;
    private String sourceNoteTitle;
    private UUID targetNoteId;
    private String targetNoteTitle;
    private NoteLinkType linkType;
    private Integer weight;
    private LocalDateTime createdAt;

    public static NoteLinkDto fromEntity(NoteLink noteLink) {
        return NoteLinkDto.builder()
            .id(noteLink.getId())
            .sourceNoteId(noteLink.getSourceNote().getId())
            .sourceNoteTitle(noteLink.getSourceNote().getTitle())
            .targetNoteId(noteLink.getTargetNote().getId())
            .targetNoteTitle(noteLink.getTargetNote().getTitle())
            .linkType(noteLink.getLinkType())
            .weight(noteLink.getWeight())
            .createdAt(noteLink.getCreatedAt())
            .build();
    }
}