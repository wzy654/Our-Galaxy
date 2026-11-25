package com.ourgalaxy.memory.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MemoryRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 20_000) String content,
        @NotNull LocalDate eventDate,
        @Size(max = 255) String locationName,
        @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
        @Pattern(regexp = "CLASSIC|GOLDEN|ROSE|BLUE") String starType,
        @Size(max = 10) List<@NotBlank @Size(max = 50) String> tags,
        @Size(max = 9) List<@Positive Long> mediaFileIds
) {
}
