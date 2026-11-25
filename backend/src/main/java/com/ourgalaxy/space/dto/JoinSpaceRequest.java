package com.ourgalaxy.space.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinSpaceRequest(@NotBlank @Size(max = 32) String code) {
}
