package com.ourgalaxy.memory.dto;

import com.ourgalaxy.file.dto.FileVO;

public record MemoryMediaVO(Long id, Long fileId, int sortOrder, FileVO file) {
}
