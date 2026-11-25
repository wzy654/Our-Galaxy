package com.ourgalaxy.space.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateSpaceRequest(
        @NotBlank @Size(max = 100) String name,
        @PastOrPresent LocalDate anniversaryDate,
        @Size(max = 500) String description
) {
}
