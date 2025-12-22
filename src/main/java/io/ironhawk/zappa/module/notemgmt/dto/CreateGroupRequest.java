package io.ironhawk.zappa.module.notemgmt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateGroupRequest {

    @NotBlank(message = "Group name is required")
    @Size(min = 1, max = 255, message = "Group name must be between 1 and 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @Size(max = 7, message = "Color must be a valid hex color code")
    private String color = "#6c757d";

    @Size(max = 50, message = "Icon class must not exceed 50 characters")
    private String icon = "fas fa-folder";

    private UUID parentGroupId;

    private Integer sortOrder = 0;
}