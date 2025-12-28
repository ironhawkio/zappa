package io.ironhawk.zappa.module.notemgmt.dto;

import io.ironhawk.zappa.module.notemgmt.entity.Group;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagCreateRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Size(max = 255, message = "Color must not exceed 255 characters")
    private String color;

    private UUID groupId; // null = global tag

    // Helper method to check if this is a global tag
    public boolean isGlobal() {
        return groupId == null;
    }
}