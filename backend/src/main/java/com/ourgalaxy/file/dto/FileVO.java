package com.ourgalaxy.file.dto;

public record FileVO(
        Long id,
        String originalName,
        String mimeType,
        long sizeBytes,
        Integer width,
        Integer height,
        String url
) {
}
