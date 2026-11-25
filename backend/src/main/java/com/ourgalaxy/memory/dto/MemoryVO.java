package com.ourgalaxy.memory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MemoryVO(
        Long id,
        String title,
        String content,
        LocalDate eventDate,
        String locationName,
        BigDecimal latitude,
        BigDecimal longitude,
        String starType,
        Long creatorId,
        String creatorName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<String> tags,
        List<MemoryMediaVO> media
) {
}
